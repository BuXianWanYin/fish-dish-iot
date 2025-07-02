package com.fishdishiot.iot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fishdishiot.iot.domain.AgricultureAutoControlStrategy;
import com.fishdishiot.iot.domain.AgricultureDeviceMqttConfig;

import java.util.List;

public interface AgricultureAutoControlStrategyService extends IService<AgricultureAutoControlStrategy> {
    List<AgricultureAutoControlStrategy> getAllActiveStrategies();
}