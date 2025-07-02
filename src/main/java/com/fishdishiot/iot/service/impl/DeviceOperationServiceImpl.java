package com.fishdishiot.iot.service.impl;

import com.fishdishiot.iot.domain.AgricultureDevice;
import com.fishdishiot.iot.domain.AjaxResult;
import com.fishdishiot.iot.service.AgricultureDeviceService;
import com.fishdishiot.iot.service.DeviceOperationService;
import com.fishdishiot.iot.service.SerialPortService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeviceOperationServiceImpl implements DeviceOperationService {

    @Autowired
    private AgricultureDeviceService deviceService;

    @Autowired
    private SerialPortService serialPortService;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DeviceOperationServiceImpl.class);

    @Override
    public AjaxResult controlDevice(Long deviceId, String action, Integer index) {
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

        if (index == 0) {
            // 单组指令逻辑：只发一次指令
            String command = null;
            if ("on".equalsIgnoreCase(action)) {
                command = onCommands[0].trim();
            } else if ("off".equalsIgnoreCase(action)) {
                command = offCommands[0].trim();
            } else {
                return AjaxResult.error(400, "操作类型错误");
            }
            int result = serialPortService.writeToSerial(serialPortService.hexStringToByteArray(command));
            if (result > 0) {
                // 指令发送成功后，更新controlStatus
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
            // 两组指令逻辑，根据 action 决定用哪一组
            if (onCommands.length < 2 || offCommands.length < 2) {
                return AjaxResult.error(400, "设备未配置多组指令");
            }
            if ("on".equalsIgnoreCase(action)) {
                // 执行开启：先发on[0]，8秒后发off[0]
                String onCommand1 = onCommands[0].trim();
                String offCommand1 = offCommands[0].trim();
                log.info("[设备操作] 执行开启，立即发送指令: {}，8秒后发送指令: {}", onCommand1, offCommand1);
                int resultOn = serialPortService.writeToSerial(serialPortService.hexStringToByteArray(onCommand1));
                if (resultOn <= 0) {
                    log.error("[设备操作] 开启指令发送失败: {}", onCommand1);
                    return AjaxResult.error(500, "开启指令发送失败");
                }
                // 指令发送成功后，更新controlStatus为1
                device.setControlStatus("1");
                deviceService.updateById(device);
                new Thread(() -> {
                    try {
                        Thread.sleep(8000);
                        log.info("[设备操作] 8秒后发送关闭指令: {}", offCommand1);
                        serialPortService.writeToSerial(serialPortService.hexStringToByteArray(offCommand1));
                    } catch (InterruptedException e) {
                        log.error("[设备操作] 发送关闭指令线程被中断", e);
                    }
                }).start();
                return AjaxResult.success("开启指令已发送，8秒后自动发送关闭指令");
            } else if ("off".equalsIgnoreCase(action)) {
                // 执行关闭：先发on[1]，8秒后发off[1]
                String onCommand2 = onCommands[1].trim();
                String offCommand2 = offCommands[1].trim();
                log.info("[设备操作] 执行关闭，立即发送指令: {}，8秒后发送指令: {}", onCommand2, offCommand2);
                int resultOff = serialPortService.writeToSerial(serialPortService.hexStringToByteArray(onCommand2));
                if (resultOff <= 0) {
                    log.error("[设备操作] 关闭指令发送失败: {}", onCommand2);
                    return AjaxResult.error(500, "关闭指令发送失败");
                }
                // 指令发送成功后，更新controlStatus为0
                device.setControlStatus("0");
                deviceService.updateById(device);
                new Thread(() -> {
                    try {
                        Thread.sleep(8000);
                        log.info("[设备操作] 8秒后发送第二组关闭指令: {}", offCommand2);
                        serialPortService.writeToSerial(serialPortService.hexStringToByteArray(offCommand2));
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
    }


}