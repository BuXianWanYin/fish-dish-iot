package com.fishdishiot.iot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fishdishiot.iot.domain.AgricultureAutoControlStrategy;
import com.fishdishiot.iot.domain.AgricultureDeviceMqttConfig;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface AgricultureAutoControlStrategyService extends IService<AgricultureAutoControlStrategy> {

    List<AgricultureAutoControlStrategy> getAllActiveStrategies();

}