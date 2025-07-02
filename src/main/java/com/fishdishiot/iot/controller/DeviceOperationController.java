package com.fishdishiot.iot.controller;

import com.fishdishiot.iot.domain.AjaxResult;
import com.fishdishiot.iot.domain.DeviceControlRequest;
import com.fishdishiot.iot.service.DeviceOperationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 设备操作控制
 */
@CrossOrigin //允许跨域
@RestController
@RequestMapping("/deviceOperation")
public class DeviceOperationController {
    @Autowired
    private DeviceOperationService deviceOperationService;

    @PostMapping("/control")
    public AjaxResult controlDevice(@RequestBody DeviceControlRequest request) {
        return deviceOperationService.controlDevice(
                request.getDeviceId(),
                request.getAction(),
                request.getIndex() == null ? 0 : request.getIndex()
        );
    }
}