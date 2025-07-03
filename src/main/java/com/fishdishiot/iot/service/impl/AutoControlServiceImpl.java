package com.fishdishiot.iot.service.impl;

import com.fishdishiot.iot.domain.AgricultureAutoControlStrategy;
import com.fishdishiot.iot.domain.AgricultureDevice;
import com.fishdishiot.iot.service.AgricultureAutoControlStrategyService;
import com.fishdishiot.iot.service.AgricultureDeviceService;
import com.fishdishiot.iot.service.AutoControlService;
import com.fishdishiot.iot.service.DeviceOperationService;
import com.fishdishiot.iot.util.SerialCommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AutoControlServiceImpl implements AutoControlService {
    private static final Logger log = LoggerFactory.getLogger(AutoControlServiceImpl.class);
    @Autowired
    private AgricultureAutoControlStrategyService strategyService;

    @Autowired
    private DeviceOperationService deviceOperationService;

    @Autowired
    private AgricultureDeviceService deviceService;

    @Autowired
    private SerialCommandExecutor serialCommandExecutor;

    /**
     * 自动检查所有启用的设备自动调节策略，并根据本次采集到的传感器数据自动执行设备操作。
     *
     * @param parsedData 解析后的传感器数据Map，key为参数名（如"temperature"），value为实际采集值
     */
    @Override
    public void checkAndExecuteStrategy(Map<String, Object> parsedData) {
        log.info("进入checkAndExecuteStrategy方法，收到数据: {}", parsedData);
        List<AgricultureAutoControlStrategy> strategies = strategyService.getAllActiveStrategies();
        log.info("[自动调节] 共检测到 {} 条启用的自动调节策略", strategies.size());

        int onIntervalMs = 300; // 300毫秒间隔

        for (AgricultureAutoControlStrategy strategy : strategies) {
            String parameter = strategy.getParameter();
            if (!parsedData.containsKey(parameter)) {
                log.info("[自动调节] 策略[ID={}] 监测参数 {} 本次数据未包含，跳过", strategy.getId(), parameter);
                continue;
            }

            Object valueObj = parsedData.get(parameter);
            if (valueObj == null) {
                log.info("[自动调节] 策略[ID={}] 监测参数 {} 本次采集值为null，跳过", strategy.getId(), parameter);
                continue;
            }

            BigDecimal value;
            try {
                value = new BigDecimal(valueObj.toString());
            } catch (Exception e) {
                log.warn("[自动调节] 策略[ID={}] 监测参数 {} 采集值 {} 转换为数字失败，跳过", strategy.getId(), parameter, valueObj);
                continue;
            }

            boolean match = false;
            String op = strategy.getConditionOperator();
            if (">".equals(op)) {
                match = value.compareTo(strategy.getConditionValue()) > 0;
            } else if ("<".equals(op)) {
                match = value.compareTo(strategy.getConditionValue()) < 0;
            } else if ("=".equals(op) || "==".equals(op)) {
                match = value.compareTo(strategy.getConditionValue()) == 0;
            } else if (">=".equals(op)) {
                match = value.compareTo(strategy.getConditionValue()) >= 0;
            } else if ("<=".equals(op)) {
                match = value.compareTo(strategy.getConditionValue()) <= 0;
            } else {
                log.warn("[自动调节] 策略[ID={}] 不支持的操作符 {}，跳过", strategy.getId(), op);
                continue;
            }

            log.info("[自动调节] 策略[ID={}] 检查参数 {} 当前值 {} 条件 {} {}，结果：{}",
                    strategy.getId(), parameter, value, op, strategy.getConditionValue(), match ? "满足" : "不满足");

            if (match) {
                Long deviceId;
                try {
                    deviceId = Long.valueOf(strategy.getDeviceId());
                } catch (Exception e) {
                    log.error("[自动调节] 策略[ID={}] 设备ID {} 转换失败，跳过", strategy.getId(), strategy.getDeviceId());
                    continue;
                }

                int index = 0;
                AgricultureDevice device = deviceService.getById(deviceId);
                if (device != null) {
                    String commandOn = device.getCommandOn();
                    String commandOff = device.getCommandOff();
                    if ((commandOn != null && commandOn.contains("|") && commandOn.split("\\|").length > 1)
                            || (commandOff != null && commandOff.contains("|") && commandOff.split("\\|").length > 1)) {
                        index = 1;
                    }
                }

                String targetStatus = "on".equalsIgnoreCase(strategy.getAction()) ? "1" : "0";
                if (device != null && targetStatus.equals(device.getControlStatus())) {
                    log.info("[自动调节] 策略[ID={}] 设备 {} 已经处于目标状态 {}，不重复执行", strategy.getId(), deviceId, targetStatus);
                    continue;
                }

                final Long finalDeviceId = deviceId;
                final String finalAction = strategy.getAction();
                final int finalIndex = index;
                final Integer duration = strategy.getExecuteDuration();
                final Long strategyId = strategy.getId();

                // on指令之间加间隔
                try {
                    Thread.sleep(onIntervalMs);
                } catch (InterruptedException ignored) {}

                serialCommandExecutor.submit(() -> {
                    log.info("[自动调节] 串行执行设备控制: deviceId={}, action={}, index={}", finalDeviceId, finalAction, finalIndex);
                    deviceOperationService.controlDevice(finalDeviceId, finalAction, finalIndex);

                    if ("on".equalsIgnoreCase(finalAction)
                            && duration != null
                            && duration > 0) {
                        log.info("[自动调节] 策略[ID={}] 设备 {} 已开启，{} 秒后将自动关闭", strategyId, finalDeviceId, duration);
                        new Thread(() -> {
                            try {
                                Thread.sleep(duration * 1000L);
                                log.info("[自动调节] 策略[ID={}] 设备 {} 到达自动关闭时间，执行关闭", strategyId, finalDeviceId);
                                deviceOperationService.controlDevice(finalDeviceId, "off", finalIndex);
                            } catch (InterruptedException ignored) {}
                        }).start();
                    }
                });
            }
        }
    }
}