package com.fishdishiot.iot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fishdishiot.iot.domain.AgricultureDeviceMqttConfig;

public interface AgricultureDeviceMqttConfigService extends IService<AgricultureDeviceMqttConfig> {
    AgricultureDeviceMqttConfig getByDeviceId(Long deviceId);
}