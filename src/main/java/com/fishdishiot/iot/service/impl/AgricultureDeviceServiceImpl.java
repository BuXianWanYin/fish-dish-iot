package com.fishdishiot.iot.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fishdishiot.iot.domain.AgricultureDevice;
import com.fishdishiot.iot.domain.AgricultureDeviceMqttConfig;
import com.fishdishiot.iot.mapper.AgricultureDeviceMapper;
import org.springframework.stereotype.Service;

@Service
public class AgricultureDeviceServiceImpl extends ServiceImpl<AgricultureDeviceMapper, AgricultureDevice>
        implements AgricultureDeviceService {
    @Override
    public AgricultureDeviceMqttConfig getByDeviceId(Long deviceId) {
        return null;
    }
}