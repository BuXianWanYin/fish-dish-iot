package com.fishdishiot.iot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fishdishiot.iot.domain.AgricultureThresholdConfig;

import java.util.List;

/**
 * 阈值配置Service接口
 *
 * @author bxwy
 * @date 2025-06-08
 */
public interface AgricultureThresholdConfigService extends IService<AgricultureThresholdConfig>
{
    /**
     * 根据设备ID获取所有启用的阈值配置
     * @param deviceId 设备ID
     * @return 阈值配置列表
     */
    List<AgricultureThresholdConfig> getEnabledConfigsByDeviceId(Long deviceId);

    /**
     * 根据设备ID和参数类型获取阈值配置
     * @param deviceId 设备ID
     * @param paramType 参数类型
     * @return 阈值配置
     */
    AgricultureThresholdConfig getConfigByDeviceIdAndParamType(Long deviceId, String paramType);
}
