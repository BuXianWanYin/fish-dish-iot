package com.fishdishiot.iot.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fishdishiot.iot.domain.AgricultureDeviceMqttConfig;
import com.fishdishiot.iot.mapper.AgricultureDeviceMqttConfigMapper;
import com.fishdishiot.iot.service.AgricultureDeviceMqttConfigService;
import org.springframework.stereotype.Service;

@Service
public class AgricultureDeviceMqttConfigServiceImpl
        extends ServiceImpl<AgricultureDeviceMqttConfigMapper, AgricultureDeviceMqttConfig>
        implements AgricultureDeviceMqttConfigService {

    @Override
    public AgricultureDeviceMqttConfig getByDeviceId(Long deviceId) {
        return lambdaQuery()
                .eq(AgricultureDeviceMqttConfig::getDeviceId, deviceId)
                .one();
    }
}