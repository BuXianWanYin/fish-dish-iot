package com.fishdishiot.iot.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fishdishiot.iot.domain.AgricultureDevice;
import com.fishdishiot.iot.util.SerialCommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 传感器通信服务
 * 核心服务，负责通过串口与传感器进行周期性的指令收发和数据解析。
 * 1. 在应用启动时，从数据库加载所有传感器设备。
 * 2. 为每个传感器创建一个独立的线程任务，进行周期性轮询。
 * 3. 发送指令给传感器，读取并解析返回的数据。
 * 4. 成功收到数据后，更新设备在Redis中的在线状态。
 * 5. 将解析后的数据交给DataProcessingService进行后续处理和存储。
 */
@Service
public class SensorCommunicationService {

    private static final Logger log = LoggerFactory.getLogger(SensorCommunicationService.class);

    @Autowired
    private SerialPortService serialPortService; // 串口服务

    @Autowired
    private DataProcessingService dataProcessingService; // 数据处理服务，用于存储解析后的数据

    @Autowired
    private AgricultureDeviceService deviceService; // 设备信息服务，用于查询设备基础信息

    @Autowired
    private AgricultureDeviceStatusService deviceStatusService; // 设备状态服务，用于更新设备实时在线状态

    @Autowired
    private SerialCommandExecutor serialCommandExecutor; // 注入 SerialCommandExecutor

    /**
     * 确保此方法在SensorCommunicationService的Bean初始化后立即执行。
     * 初始化线程池。
     */
    @PostConstruct
    public void init() {
        log.info("正在初始化传感器通信服务...");
    }

    /**
     * 监听应用就绪事件（ApplicationReadyEvent）。
     * spring boot应用完全启动并准备好接收请求时，调用此方法。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("应用程序已就绪，正在启动传感器通信...");
        startSensorCommunication();
    }

    /**
     * Spring容器销毁SensorCommunicationService的Bean之前调用。
     * 执行资源清理工作，优雅地关闭线程池。
     */
    @PreDestroy
    public void destroy() {
        log.info("正在关闭传感器通信服务...");
    }

    /**
     * 启动总的传感器通信流程。
     * 查询所有需要轮询的传感器，并逐一启动任务。
     */
    private void startSensorCommunication() {
        try {
            // 查询所有传感器设备
            List<AgricultureDevice> sensors = getSensorDevices();

            for (AgricultureDevice sensor : sensors) {
                // 跳过未开启的设备
//                if (!"1".equals(sensor.getControlStatus())) {
//                    log.info("传感器 {} (ID: {}) 用户已关闭，跳过采集任务。", sensor.getDeviceName(), sensor.getId());
//                    continue;
//                }
                // 跳过未配置指令的设备
                String command = sensor.getSensorCommand();
                if (command == null || "null".equals(command)) {
                    log.warn("传感器 {} (ID: {}) 没有配置指令，跳过任务启动。", sensor.getDeviceName(), sensor.getId());
                    continue;
                }
                // 启动任务
                startSensorCollectLoop(sensor);
            }
        } catch (Exception e) {
            log.error("启动传感器通信流程失败", e);
        }
    }

    /**
     * 从数据库获取所有需要轮询的传感器设备列表。
     * @return 传感器设备列表
     */
    private List<AgricultureDevice> getSensorDevices() {
        QueryWrapper<AgricultureDevice> queryWrapper = new QueryWrapper<>();
        // '1' 气象传感器, '2' 水质传感器, '6' 其他传感器
        queryWrapper.in("device_type_id", Arrays.asList("1", "2", "6"));
        return deviceService.list(queryWrapper);
    }

    /**
     * 启动单个传感器的采集循环（通过全局队列串行化）。
     */
    private void startSensorCollectLoop(AgricultureDevice sensor) {
        new Thread(() -> {
            String sensorName = sensor.getDeviceName();
            String commandHexStr = sensor.getSensorCommand();
            Long sensorId = sensor.getId();
            log.info("线程 {} 正在启动，负责轮询传感器: {} (ID: {})", Thread.currentThread().getName(), sensorName, sensorId);
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    serialCommandExecutor.submit(() -> {
                        synchronized (serialPortService.getSerialLock()) {
                            try {
                                byte[] commandBytes = hexStringToByteArray(commandHexStr);
                                serialPortService.writeToSerial(commandBytes);
                                Thread.sleep(200);
                                byte[] response = serialPortService.readFromSerial(256);
                                if (response != null && response.length > 0) {
                                    deviceStatusService.updateDeviceOnline(sensor.getId().toString());
                                    String deviceType = sensor.getDeviceTypeId();
                                    Map<String, Object> parsedData = parseSensorData(response, deviceType);
                                    parsedData.put("deviceId", sensorId);
                                    parsedData.put("deviceName", sensorName);
                                    parsedData.put("type", getDataTypeByDeviceType(deviceType));
                                    parsedData.put("pastureId", sensor.getPastureId());
                                    parsedData.put("batchId", sensor.getBatchId());
                                    log.info("成功接收并解析来自 {} (ID: {}) 的数据: {}", sensorName, sensorId, parsedData);
                                    dataProcessingService.processAndStore(parsedData);
                                } else {
                                    log.warn("轮询 {} (ID: {}) 未收到响应。", sensorName, sensorId);
                                }
                            } catch (Exception e) {
                                log.error("采集任务异常: {}", e.getMessage(), e);
                            }
                        }
                    });
                    Thread.sleep(5000); // 轮询间隔
                } catch (InterruptedException e) {
                    break;
                }
            }
            log.info("传感器 {} (ID: {}) 的轮询任务已完全停止。", sensorName, sensorId);
        }, "Sensor-Collect-" + sensor.getId()).start();
    }

    /**
     * 数据解析的主分发方法。
     * 根据设备类型，调用相应的具体解析方法。
     * @param data 从串口读取的原始字节数组
     * @param deviceType 设备的类型ID ("1", "2", "6")
     * @return 包含解析后键值对的Map
     */
    private Map<String, Object> parseSensorData(byte[] data, String deviceType) {
        Map<String, Object> result = new HashMap<>();
        try {
            switch (deviceType) {
                case "1": // 气象传感器
                    result.putAll(parseWeatherSensorData(data));
                    break;
                case "2": // 水质传感器
                    result.putAll(parseWaterQualityData(data));
                    break;
                case "6": // 其他传感器
                    result.putAll(parseOtherSensorData(data));
                    break;
                default:
                    result.put("raw_data", bytesToHexString(data));
            }
        } catch (Exception e) {
            log.error("解析传感器数据出错: {}", e.getMessage(), e);
        }
        return result;
    }

    /**
     * 解析气象传感器数据
     */
    private Map<String, Object> parseWeatherSensorData(byte[] data) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (data.length >= 19) {
                // 百叶箱数据
                double humidity = Math.round(((data[3] & 0xFF) << 8 | (data[4] & 0xFF)) / 10.0 * 10.0) / 10.0;
                double temperature = Math.round(((data[5] & 0xFF) << 8 | (data[6] & 0xFF)) / 10.0 * 10.0) / 10.0;
                double noise = Math.round(((data[7] & 0xFF) << 8 | (data[8] & 0xFF)) / 10.0 * 10.0) / 10.0;
                int pm25 = (data[9] & 0xFF) << 8 | (data[10] & 0xFF);
                int pm10 = (data[13] & 0xFF) << 8 | (data[14] & 0xFF);
                int light = (data[17] & 0xFF) << 8 | (data[18] & 0xFF);
                
                result.put("humidity", humidity);
                result.put("temperature", temperature);
                result.put("noise", noise);
                result.put("pm25", pm25);
                result.put("pm10", pm10);
                result.put("light_intensity", light);
            } else if (data.length >= 7) {
                // 风向或风速数据
                if (data[0] == 0x01) {
                    // 风向数据
                    int directionGrade = ((data[3] & 0xFF) << 8) | (data[4] & 0xFF);
                    int directionAngle = ((data[5] & 0xFF) << 8) | (data[6] & 0xFF);
                    
                    String[] directions = {"北风", "东北风", "东风", "东南风", "南风", "西南风", "西风", "西北风"};
                    String direction = (directionGrade >= 0 && directionGrade < directions.length) 
                        ? directions[directionGrade] : "未知风向";
                    
                    result.put("wind_direction", direction);
                    result.put("direction_angle", directionAngle);
                } else if (data[0] == 0x03) {
                    // 风速数据
                    int speedValue = (data[3] & 0xFF) << 8 | (data[4] & 0xFF);
                    double speed = speedValue / 10.0;
                    result.put("wind_speed", speed);
                }
            }
        } catch (Exception e) {
            log.error("解析气象传感器数据出错: {}", e.getMessage(), e);
        }
        return result;
    }

    /**
     * 解析水质传感器数据
     */
    private Map<String, Object> parseWaterQualityData(byte[] data) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (data.length >= 9) {
                // 解析温度
                int tempValue = ((data[3] & 0xFF) << 8) | (data[4] & 0xFF);
                int tempDecimal = ((data[5] & 0xFF) << 8) | (data[6] & 0xFF);
                double temperature = Math.round((tempValue / Math.pow(10, tempDecimal)) * Math.pow(10, tempDecimal)) / Math.pow(10, tempDecimal);
                
                // 解析pH值
                double phValue = ((data[7] & 0xFF) << 8 | (data[8] & 0xFF)) / 100.0;
                
                result.put("water_temperature", temperature);
                result.put("ph_value", phValue);
            }
        } catch (Exception e) {
            log.error("解析水质传感器数据出错: {}", e.getMessage(), e);
        }
        return result;
    }

    /**
     * 解析其他传感器数据
     */
    private Map<String, Object> parseOtherSensorData(byte[] data) {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("raw_data", bytesToHexString(data));
        } catch (Exception e) {
            log.error("解析其他传感器数据出错: {}", e.getMessage(), e);
        }
        return result;
    }

    /**
     * 将16进制表示的字符串（例如 "01 0A"）转换为字节数组（例如 {0x01, 0x0A}）。
     * 会自动忽略空格。
     * @param hex 16进制字符串
     * @return 对应的字节数组
     */
    byte[] hexStringToByteArray(String hex) {
        hex = hex.replaceAll("\\s", "");
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * 将字节数组转换为16进制表示的字符串，每个字节后附带一个空格，便于阅读。
     * @param bytes 字节数组
     * @return 格式化的16进制字符串
     */
    String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    /**
     * 解析风向传感器数据
     */
    Map<String, Object> parseWindDirectionData(byte[] data) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (data.length >= 7) {
                int directionGrade = ((data[3] & 0xFF) << 8) | (data[4] & 0xFF);
                int directionAngle = ((data[5] & 0xFF) << 8) | (data[6] & 0xFF);
                
                String[] directions = {"北风", "东北风", "东风", "东南风", "南风", "西南风", "西风", "西北风"};
                String direction = (directionGrade >= 0 && directionGrade < directions.length) 
                    ? directions[directionGrade] : "未知风向";
                
                result.put("direction_grade", directionGrade);
                result.put("direction_angle", directionAngle);
                result.put("direction", direction);
                result.put("wind_direction", direction);
            }
        } catch (Exception e) {
            log.error("解析风向数据出错: {}", e.getMessage(), e);
        }
        return result;
    }

    /**
     * 解析百叶箱数据
     */
    Map<String, Object> parseBaiyeBoxData(byte[] data) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (data.length >= 19) {
                double humidity = Math.round(((data[3] & 0xFF) << 8 | (data[4] & 0xFF)) / 10.0 * 10.0) / 10.0;
                double temperature = Math.round(((data[5] & 0xFF) << 8 | (data[6] & 0xFF)) / 10.0 * 10.0) / 10.0;
                double noise = Math.round(((data[7] & 0xFF) << 8 | (data[8] & 0xFF)) / 10.0 * 10.0) / 10.0;
                int pm25 = (data[9] & 0xFF) << 8 | (data[10] & 0xFF);
                int pm10 = (data[13] & 0xFF) << 8 | (data[14] & 0xFF);
                int light = (data[17] & 0xFF) << 8 | (data[18] & 0xFF);
                
                result.put("humidity", humidity);
                result.put("temperature", temperature);
                result.put("noise", noise);
                result.put("pm25", pm25);
                result.put("pm10", pm10);
                result.put("light", light);
                result.put("light_intensity", light);
            }
        } catch (Exception e) {
            log.error("解析百叶箱数据出错: {}", e.getMessage(), e);
        }
        return result;
    }

    /**
     * 解析风速传感器数据
     */
    Map<String, Object> parseWindSpeedData(byte[] data) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (data.length >= 5) {
                int speedValue = (data[3] & 0xFF) << 8 | (data[4] & 0xFF);
                double speed = speedValue / 10.0;
                result.put("speed", speed);
                result.put("wind_speed", speed);
            }
        } catch (Exception e) {
            log.error("解析风速数据出错: {}", e.getMessage(), e);
        }
        return result;
    }

    /**
     * 根据设备类型ID，返回一个更通用的数据分类（"weather" 或 "water"）。
     * 这个分类用于数据处理服务和MQTT主题。
     * @param deviceType 设备类型ID
     * @return 数据分类字符串
     */
    String getDataTypeByDeviceType(String deviceType) {
        switch (deviceType) {
            case "1":
                return "weather";
            case "2":
                return "water";
            case "6":
                return "other";
            default:
                return "unknown";
        }
    }

    /**
     * 公开方法，用于外部触发重新加载传感器配置。
     * 它会先停止所有当前任务，然后重新从数据库加载并启动任务。
     */
    public void reloadSensorConfig() {
        log.info("正在重新加载传感器配置...");
        // stopAllSensorTasks(); // 移除
        startSensorCommunication();
    }

    /**
     * 获取当前所有传感器任务的运行状态。
     * @return 一个Map，Key是设备ID，Value是任务状态（"RUNNING" 或 "STOPPED"）。
     */
    public Map<String, String> getSensorTaskStatus() {
        return new HashMap<>();
    }
} 