package com.fishdishiot.iot.service;

import java.util.List;
import java.util.Map;

/**
 * 环境趋势分析服务
 * 负责分析24小时内的水质和气象数据趋势
 */
public interface EnvironmentTrendAnalysisService {

    /**
     * 趋势类型枚举
     */
    enum TrendType {
        STABLE("稳定"),
        RISING("上升"),
        FALLING("下降");

        private final String description;

        TrendType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    Map<String, Object> getWaterQualityDeviceInfo();

    Map<String, Object> getWeatherDeviceInfo();

    Map<String, Object> analyzeWaterQualityTrends();

    Map<String, Object> analyzeWeatherTrends();
}