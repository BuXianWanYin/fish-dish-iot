package com.fishdishiot.iot.service.impl;

import com.fishdishiot.iot.domain.AgricultureDevice;
import com.fishdishiot.iot.domain.AjaxResult;
import com.fishdishiot.iot.service.AgricultureDeviceService;
import com.fishdishiot.iot.service.DeviceOperationService;
import com.fishdishiot.iot.service.SerialPortService;
import com.fishdishiot.iot.service.SensorCommunicationService;
import com.fishdishiot.iot.util.SerialCommandExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Service
public class DeviceOperationServiceImpl implements DeviceOperationService {

    @Autowired
    private AgricultureDeviceService deviceService;

    @Autowired
    private SerialPortService serialPortService;

    @Autowired
    private SerialCommandExecutor serialCommandExecutor;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DeviceOperationServiceImpl.class);

    @Override
    public AjaxResult controlDevice(Long deviceId, String action, Integer index) {
        log.info("[调试专用] DeviceOperationServiceImpl的controlDevice被调用");
        AgricultureDevice device = deviceService.getById(deviceId);
        if (device == null) {
            return AjaxResult.error(404, "设备不存在");
        }
        if (device.getIsControllable() == null || !"1".equals(device.getIsControllable())) {
            return AjaxResult.error(403, "该设备不可控");
        }

        String commandOnStr = device.getCommandOn();
        String commandOffStr = device.getCommandOff();

        if (commandOnStr == null || commandOnStr.trim().isEmpty()) {
            return AjaxResult.error(400, "设备未配置开启指令");
        }
        if (commandOffStr == null || commandOffStr.trim().isEmpty()) {
            return AjaxResult.error(400, "设备未配置关闭指令");
        }

        String[] onCommands = commandOnStr.split("\\|");
        String[] offCommands = commandOffStr.split("\\|");

        // 死锁修复：如果当前线程就是Serial-Command-Executor，直接执行
        if (Thread.currentThread().getName().equals("Serial-Command-Executor")) {
            return controlDeviceDirect(device, action, index, onCommands, offCommands);
        }

        try {
            if (index == 0) {
                final String command;
                if ("on".equalsIgnoreCase(action)) {
                    command = onCommands[0].trim();
                } else if ("off".equalsIgnoreCase(action)) {
                    command = offCommands[0].trim();
                } else {
                    return AjaxResult.error(400, "操作类型错误");
                }
                Future<Integer> future = serialCommandExecutor.submit(() -> {
                    synchronized (serialPortService.getSerialLock()) {
                        return serialPortService.writeToSerial(serialPortService.hexStringToByteArray(command));
                    }
                });
                int result = future.get();
                if (result > 0) {
                    if ("on".equalsIgnoreCase(action)) {
                        device.setControlStatus("1");
                    } else if ("off".equalsIgnoreCase(action)) {
                        device.setControlStatus("0");
                    }
                    deviceService.updateById(device);
                    return AjaxResult.success("指令发送成功");
                } else {
                    return AjaxResult.error(500, "指令发送失败");
                }
            } else if (index == 1) {
                if (onCommands.length < 2 || offCommands.length < 2) {
                    return AjaxResult.error(400, "设备未配置多组指令");
                }
                if ("on".equalsIgnoreCase(action)) {
                    final String onCommand1 = onCommands[0].trim();
                    final String offCommand1 = offCommands[0].trim();
                    log.info("[设备操作] 执行开启，立即发送指令: {}，8秒后发送指令: {}", onCommand1, offCommand1);
                    Future<Integer> futureOn = serialCommandExecutor.submit(() -> {
                        synchronized (serialPortService.getSerialLock()) {
                            return serialPortService.writeToSerial(serialPortService.hexStringToByteArray(onCommand1));
                        }
                    });
                    int resultOn = futureOn.get();
                    if (resultOn <= 0) {
                        log.error("[设备操作] 开启指令发送失败: {}", onCommand1);
                        return AjaxResult.error(500, "开启指令发送失败");
                    }
                    device.setControlStatus("1");
                    deviceService.updateById(device);
                    new Thread(() -> {
                        try {
                            Thread.sleep(8000);
                            log.info("[设备操作] 8秒后发送关闭指令: {}", offCommand1);
                            serialCommandExecutor.submit(() -> {
                                synchronized (serialPortService.getSerialLock()) {
                                    serialPortService.writeToSerial(serialPortService.hexStringToByteArray(offCommand1));
                                }
                                return null;
                            });
                        } catch (InterruptedException e) {
                            log.error("[设备操作] 发送关闭指令线程被中断", e);
                        }
                    }).start();
                    return AjaxResult.success("开启指令已发送，8秒后自动发送关闭指令");
                } else if ("off".equalsIgnoreCase(action)) {
                    final String onCommand2 = onCommands[1].trim();
                    final String offCommand2 = offCommands[1].trim();
                    log.info("[设备操作] 执行关闭，立即发送指令: {}，8秒后发送指令: {}", onCommand2, offCommand2);
                    Future<Integer> futureOff = serialCommandExecutor.submit(() -> {
                        synchronized (serialPortService.getSerialLock()) {
                            return serialPortService.writeToSerial(serialPortService.hexStringToByteArray(onCommand2));
                        }
                    });
                    int resultOff = futureOff.get();
                    if (resultOff <= 0) {
                        log.error("[设备操作] 关闭指令发送失败: {}", onCommand2);
                        return AjaxResult.error(500, "关闭指令发送失败");
                    }
                    device.setControlStatus("0");
                    deviceService.updateById(device);
                    new Thread(() -> {
                        try {
                            Thread.sleep(8000);
                            log.info("[设备操作] 8秒后发送第二组关闭指令: {}", offCommand2);
                            serialCommandExecutor.submit(() -> {
                                synchronized (serialPortService.getSerialLock()) {
                                    serialPortService.writeToSerial(serialPortService.hexStringToByteArray(offCommand2));
                                }
                                return null;
                            });
                        } catch (InterruptedException e) {
                            log.error("[设备操作] 发送第二组关闭指令线程被中断", e);
                        }
                    }).start();
                    return AjaxResult.success("关闭指令已发送，8秒后自动发送第二组关闭指令");
                } else {
                    return AjaxResult.error(400, "操作类型错误");
                }
            } else {
                return AjaxResult.error(400, "不支持的指令索引");
            }
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.error("[设备操作] 控制指令执行异常", e);
            return AjaxResult.error(500, "控制指令执行异常");
        }
    }

    // 新增：直接串口操作的方法，避免死锁
    private AjaxResult controlDeviceDirect(AgricultureDevice device, String action, Integer index, String[] onCommands, String[] offCommands) {
        try {
            if (index == 0) {
                final String command;
                if ("on".equalsIgnoreCase(action)) {
                    command = onCommands[0].trim();
                } else if ("off".equalsIgnoreCase(action)) {
                    command = offCommands[0].trim();
                } else {
                    return AjaxResult.error(400, "操作类型错误");
                }
                int result;
                synchronized (serialPortService.getSerialLock()) {
                    result = serialPortService.writeToSerial(serialPortService.hexStringToByteArray(command));
                }
                if (result > 0) {
                    if ("on".equalsIgnoreCase(action)) {
                        device.setControlStatus("1");
                    } else if ("off".equalsIgnoreCase(action)) {
                        device.setControlStatus("0");
                    }
                    deviceService.updateById(device);
                    return AjaxResult.success("指令发送成功");
                } else {
                    return AjaxResult.error(500, "指令发送失败");
                }
            } else if (index == 1) {
                if (onCommands.length < 2 || offCommands.length < 2) {
                    return AjaxResult.error(400, "设备未配置多组指令");
                }
                if ("on".equalsIgnoreCase(action)) {
                    final String onCommand1 = onCommands[0].trim();
                    final String offCommand1 = offCommands[0].trim();
                    log.info("[设备操作] (direct) 执行开启，立即发送指令: {}，8秒后发送指令: {}", onCommand1, offCommand1);
                    int resultOn;
                    synchronized (serialPortService.getSerialLock()) {
                        resultOn = serialPortService.writeToSerial(serialPortService.hexStringToByteArray(onCommand1));
                    }
                    if (resultOn <= 0) {
                        log.error("[设备操作] (direct) 开启指令发送失败: {}", onCommand1);
                        return AjaxResult.error(500, "开启指令发送失败");
                    }
                    device.setControlStatus("1");
                    deviceService.updateById(device);
                    new Thread(() -> {
                        try {
                            Thread.sleep(8000);
                            log.info("[设备操作] (direct) 8秒后发送关闭指令: {}", offCommand1);
                            synchronized (serialPortService.getSerialLock()) {
                                serialPortService.writeToSerial(serialPortService.hexStringToByteArray(offCommand1));
                            }
                        } catch (InterruptedException e) {
                            log.error("[设备操作] (direct) 发送关闭指令线程被中断", e);
                        }
                    }).start();
                    return AjaxResult.success("开启指令已发送，8秒后自动发送关闭指令");
                } else if ("off".equalsIgnoreCase(action)) {
                    final String onCommand2 = onCommands[1].trim();
                    final String offCommand2 = offCommands[1].trim();
                    log.info("[设备操作] (direct) 执行关闭，立即发送指令: {}，8秒后发送指令: {}", onCommand2, offCommand2);
                    int resultOff;
                    synchronized (serialPortService.getSerialLock()) {
                        resultOff = serialPortService.writeToSerial(serialPortService.hexStringToByteArray(onCommand2));
                    }
                    if (resultOff <= 0) {
                        log.error("[设备操作] (direct) 关闭指令发送失败: {}", onCommand2);
                        return AjaxResult.error(500, "关闭指令发送失败");
                    }
                    device.setControlStatus("0");
                    deviceService.updateById(device);
                    new Thread(() -> {
                        try {
                            Thread.sleep(8000);
                            log.info("[设备操作] (direct) 8秒后发送第二组关闭指令: {}", offCommand2);
                            synchronized (serialPortService.getSerialLock()) {
                                serialPortService.writeToSerial(serialPortService.hexStringToByteArray(offCommand2));
                            }
                        } catch (InterruptedException e) {
                            log.error("[设备操作] (direct) 发送第二组关闭指令线程被中断", e);
                        }
                    }).start();
                    return AjaxResult.success("关闭指令已发送，8秒后自动发送第二组关闭指令");
                } else {
                    return AjaxResult.error(400, "操作类型错误");
                }
            } else {
                return AjaxResult.error(400, "不支持的指令索引");
            }
        } catch (Exception e) {
            log.error("[设备操作] (direct) 控制指令执行异常", e);
            return AjaxResult.error(500, "控制指令执行异常");
        }
    }


}