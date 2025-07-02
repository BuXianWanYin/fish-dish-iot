package com.fishdishiot.iot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fishdishiot.iot.domain.AgricultureAutoControlStrategy;
import com.fishdishiot.iot.mapper.AgricultureAutoControlStrategyMapper;
import com.fishdishiot.iot.service.AgricultureAutoControlStrategyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgricultureAutoControlStrategyServiceImpl
extends ServiceImpl<AgricultureAutoControlStrategyMapper, AgricultureAutoControlStrategy>
implements AgricultureAutoControlStrategyService {

    @Autowired
    private AgricultureAutoControlStrategyMapper strategyMapper;

    //查询所有启用的自动策略
    @Override
    public List<AgricultureAutoControlStrategy> getAllActiveStrategies() {
        QueryWrapper<AgricultureAutoControlStrategy> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1); // 只查启用的
        return strategyMapper.selectList(wrapper);
    }
}
