package com.fishdishiot.iot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fishdishiot.iot.domain.AgricultureDevice;
import com.fishdishiot.iot.domain.AgricultureDeviceMqttConfig;

public interface AgricultureDeviceService extends IService<AgricultureDevice> {
    AgricultureDeviceMqttConfig getByDeviceId(Long deviceId);
}