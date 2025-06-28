package com.fishdishiot.iot.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fishdishiot.iot.domain.AgricultureThresholdConfig;
import com.fishdishiot.iot.mapper.AgricultureThresholdConfigMapper;
import com.fishdishiot.iot.service.AgricultureThresholdConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(AgricultureThresholdConfigServiceImpl.class);

    @Override
    public List<AgricultureThresholdConfig> getEnabledConfigsByDeviceId(Long deviceId) {
        List<AgricultureThresholdConfig> configs = lambdaQuery()
                .eq(AgricultureThresholdConfig::getDeviceId, deviceId)
                .eq(AgricultureThresholdConfig::getIsEnabled, true)
                .list();
        if (configs != null && !configs.isEmpty()) {
            log.info("查到设备ID {} 的启用阈值配置: {}", deviceId, configs);
        }
        return configs;
    }

    @Override
    public AgricultureThresholdConfig getConfigByDeviceIdAndParamType(Long deviceId, String paramType) {
        AgricultureThresholdConfig config = lambdaQuery()
                .eq(AgricultureThresholdConfig::getDeviceId, deviceId)
                .eq(AgricultureThresholdConfig::getParamType, paramType)
                .eq(AgricultureThresholdConfig::getIsEnabled, true)
                .one();
        if (config != null) {
            log.info("查到设备ID {} 参数类型 {} 的阈值配置: {}", deviceId, paramType, config);
        }
        return config;
    }
}
