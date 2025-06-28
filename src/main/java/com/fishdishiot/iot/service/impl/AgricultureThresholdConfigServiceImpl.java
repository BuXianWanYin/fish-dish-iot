package com.fishdishiot.iot.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fishdishiot.iot.domain.AgricultureThresholdConfig;
import com.fishdishiot.iot.mapper.AgricultureThresholdConfigMapper;
import com.fishdishiot.iot.service.AgricultureThresholdConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 阈值配置Service业务层处理
 * 
 * @author server
 * @date 2025-06-08
 */
@Service
public class AgricultureThresholdConfigServiceImpl extends ServiceImpl<AgricultureThresholdConfigMapper, AgricultureThresholdConfig> implements AgricultureThresholdConfigService
{
    @Override
    public List<AgricultureThresholdConfig> getEnabledConfigsByDeviceId(Long deviceId) {
        return lambdaQuery()
                .eq(AgricultureThresholdConfig::getDeviceId, deviceId)
                .eq(AgricultureThresholdConfig::getIsEnabled, true)
                .list();
    }

    @Override
    public AgricultureThresholdConfig getConfigByDeviceIdAndParamType(Long deviceId, String paramType) {
        return lambdaQuery()
                .eq(AgricultureThresholdConfig::getDeviceId, deviceId)
                .eq(AgricultureThresholdConfig::getParamType, paramType)
                .eq(AgricultureThresholdConfig::getIsEnabled, true)
                .one();
    }
}
