package com.fishdishiot.iot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fishdishiot.iot.domain.AgricultureAutoControlStrategy;
import com.fishdishiot.iot.domain.AgricultureDevice;
import com.fishdishiot.iot.mapper.AgricultureAutoControlStrategyMapper;
import com.fishdishiot.iot.service.AgricultureAutoControlStrategyService;
import com.fishdishiot.iot.service.AgricultureDeviceService;
import com.fishdishiot.iot.service.DeviceOperationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class AgricultureAutoControlStrategyServiceImpl
        extends ServiceImpl<AgricultureAutoControlStrategyMapper, AgricultureAutoControlStrategy>
        implements AgricultureAutoControlStrategyService {

    @Autowired
    private AgricultureAutoControlStrategyMapper strategyMapper;

    @Override
    public List<AgricultureAutoControlStrategy> getAllActiveStrategies() {
        QueryWrapper<AgricultureAutoControlStrategy> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1); // 只查启用的
        return strategyMapper.selectList(wrapper);
    }

}
