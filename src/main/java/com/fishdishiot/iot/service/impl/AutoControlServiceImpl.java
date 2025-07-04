package com.fishdishiot.iot.service.impl;

import com.fishdishiot.iot.domain.AgricultureAutoControlStrategy;
import com.fishdishiot.iot.domain.AgricultureDevice;
import com.fishdishiot.iot.service.*;
import com.fishdishiot.iot.util.SerialCommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    @Autowired
    private SerialPortService serialPortService;

    // 防抖动状态记录
    private final Map<Long, Boolean> lastTriggerMap = new ConcurrentHashMap<>();
    private final Map<Long, Long> lastOffTimeMap = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MILLIS = 120 * 1000;  // 60秒冷却期

    /**
     * 自动检查所有启用的设备自动调节策略，并根据本次采集到的传感器数据自动执行设备操作。
     *
     * @param parsedData 解析后的传感器数据Map，key为参数名（如"temperature"），value为实际采集值
     */
    @Override
    public void checkAndExecuteStrategy(Map<String, Object> parsedData) {
        List<AgricultureAutoControlStrategy> strategies = strategyService.getAllActiveStrategies();
        log.info("[自动调节] 共检测到 {} 条启用的自动调节策略", strategies.size());

        int onIntervalMs = 1000; // 300毫秒间隔

        // 先收集所有本轮需要on的任务
        List<Runnable> onTasks = new ArrayList<>();

        for (AgricultureAutoControlStrategy strategy : strategies) {
            String parameter = strategy.getParameter();
            if (!parsedData.containsKey(parameter)) continue;
            Object valueObj = parsedData.get(parameter);
            if (valueObj == null) continue;

            BigDecimal value;
            try { value = new BigDecimal(valueObj.toString()); }
            catch (Exception e) { continue; }

            boolean match = false;
            String op = strategy.getConditionOperator();
            if (">".equals(op)) match = value.compareTo(strategy.getConditionValue()) > 0;
            else if ("<".equals(op)) match = value.compareTo(strategy.getConditionValue()) < 0;
            else if ("=".equals(op) || "==".equals(op)) match = value.compareTo(strategy.getConditionValue()) == 0;
            else if (">=".equals(op)) match = value.compareTo(strategy.getConditionValue()) >= 0;
            else if ("<=".equals(op)) match = value.compareTo(strategy.getConditionValue()) <= 0;
            else continue;

            Long deviceId;
            try { deviceId = Long.valueOf(strategy.getDeviceId()); }
            catch (Exception e) { continue; }

            int index = 0;
            AgricultureDevice device = deviceService.getById(deviceId);
            if (device != null) {
                String commandOn = device.getCommandOn();
                String commandOff = device.getCommandOff();
                if ((commandOn != null && commandOn.contains("|") && commandOn.split("\\|").length > 1)
                        || (commandOff != null && commandOff.split("\\|").length > 1)) {
                    index = 1;
                }
            }

            String targetStatus = "on".equalsIgnoreCase(strategy.getAction()) ? "1" : "0";
            if (device != null && targetStatus.equals(device.getControlStatus())) continue;

            // 防抖动逻辑
            Long lastOffTime = lastOffTimeMap.get(deviceId);
            if (lastOffTime != null && System.currentTimeMillis() - lastOffTime < COOLDOWN_MILLIS) {
                log.info("[自动调节] 设备 {} 处于冷却期，跳过自动开启", deviceId);
                continue;
            }

            // 只在未触发->触发时执行on
            Boolean lastTriggered = lastTriggerMap.getOrDefault(deviceId, false);
            if (!(match && !lastTriggered)) continue;
            lastTriggerMap.put(deviceId, true);

            final Long finalDeviceId = deviceId;
            final String finalAction = strategy.getAction();
            final int finalIndex = index;
            final Integer duration = strategy.getExecuteDuration();
            final Long strategyId = strategy.getId();

            onTasks.add(() -> {
                log.info("[自动调节] 串行执行设备控制: deviceId={}, action={}, index={}", finalDeviceId, finalAction, finalIndex);
                synchronized (serialPortService.getSerialLock()) {
                    deviceOperationService.controlDevice(finalDeviceId, finalAction, finalIndex);
                }

                if ("on".equalsIgnoreCase(finalAction)
                        && duration != null
                        && duration > 0) {
                    log.info("[自动调节] 策略[ID={}] 设备 {} 已开启，{} 秒后将自动关闭", strategyId, finalDeviceId, duration);
                    new Thread(() -> {
                        try {
                            Thread.sleep(duration * 1000L);
                            log.info("[自动调节] 策略[ID={}] 设备 {} 到达自动关闭时间，异步提交关闭任务", strategyId, finalDeviceId);
                            //异步关闭 关闭任务
                            serialCommandExecutor.submit(() -> {
                                deviceOperationService.controlDevice(finalDeviceId, "off", finalIndex);
                                lastOffTimeMap.put(finalDeviceId, System.currentTimeMillis());
                                lastTriggerMap.put(finalDeviceId, false);
                            });
                        } catch (InterruptedException ignored) {}
                    }).start();
                }
            });
        }

        // 依次（带间隔）提交on任务到队列
        for (int i = 0; i < onTasks.size(); i++) {
            Runnable task = onTasks.get(i);
            boolean isLast = (i == onTasks.size() - 1);
            serialCommandExecutor.submit(() -> {
                task.run();
                if (!isLast) {
                    try { Thread.sleep(onIntervalMs); } catch (InterruptedException ignored) {}
                }
            });
        }
    }
}