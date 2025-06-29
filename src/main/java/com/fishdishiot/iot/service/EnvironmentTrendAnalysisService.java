package com.fishdishiot.iot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fishdishiot.iot.domain.AgricultureWaterQualityData;
import com.fishdishiot.iot.domain.AgricultureWeatherData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 环境趋势分析服务
 * 负责分析24小时内的水质和气象数据趋势
 */
@Slf4j
@Service
public class EnvironmentTrendAnalysisService {

    @Autowired
    private AgricultureWaterQualityDataService waterQualityDataService;

    @Autowired
    private AgricultureWeatherDataService weatherDataService;

    /**
     * 趋势类型枚举
     * STABLE: 数据相对稳定，变化率在阈值范围内
     * RISING: 数据呈上升趋势，变化率超过正向阈值
     * FALLING: 数据呈下降趋势，变化率超过负向阈值
     */
    public enum TrendType {
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

    public Map<String, Object> getWaterQualityDeviceInfo() {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(24);

        LambdaQueryWrapper<AgricultureWaterQualityData> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(AgricultureWaterQualityData::getCollectTime, startTime, endTime)
                .orderByDesc(AgricultureWaterQualityData::getCollectTime)
                .last("LIMIT 1"); // 获取最新的一条记录

        AgricultureWaterQualityData latestData = waterQualityDataService.getOne(wrapper);
        
        Map<String, Object> deviceInfo = new HashMap<>();
        if (latestData != null) {
            deviceInfo.put("deviceId", latestData.getDeviceId());
            deviceInfo.put("pastureId", latestData.getPastureId());
            deviceInfo.put("batchId", latestData.getBatchId());
        }
        return deviceInfo;
    }

    /**
     * 获取气象监测设备信息
     */
    public Map<String, Object> getWeatherDeviceInfo() {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(24);

        LambdaQueryWrapper<AgricultureWeatherData> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(AgricultureWeatherData::getCollectTime, startTime, endTime)
                .orderByDesc(AgricultureWeatherData::getCollectTime)
                .last("LIMIT 1"); // 获取最新的一条记录

        AgricultureWeatherData latestData = weatherDataService.getOne(wrapper);
        
        Map<String, Object> deviceInfo = new HashMap<>();
        if (latestData != null) {
            deviceInfo.put("deviceId", latestData.getDeviceId());
            deviceInfo.put("pastureId", latestData.getPastureId());
            deviceInfo.put("batchId", latestData.getBatchId());
        }
        return deviceInfo;
    }
    
    /**
     * 通用趋势分析方法
     * @param data 待分析的数据列表
     * @param valueExtractor 数据值提取器函数
     * @return 包含趋势分析结果的Map，包括：
     *         - trend: 趋势类型（稳定/上升/下降）
     *         - confidence: 置信度（0-100%）
     *         - changeRate: 变化率（百分比）
     */
    private <T> Map<String, Object> analyzeTrend(List<T> data, java.util.function.Function<T, Double> valueExtractor) {
        Map<String, Object> result = new HashMap<>();

        // 处理空数据情况
        if (data == null || data.isEmpty()) {
            result.put("trend", TrendType.STABLE.getDescription());
            result.put("confidence", 0.0);
            return result;
        }

        // 提取有效的数值数据
        List<Double> values = new ArrayList<>();
        for (T item : data) {
            Double value = valueExtractor.apply(item);
            if (value != null) {
                values.add(value);
            }
        }

        // 数据点不足以分析趋势
        if (values.size() < 2) {
            result.put("trend", TrendType.STABLE.getDescription());
            result.put("confidence", 0.0);
            return result;
        }

        // 计算首尾值的变化
        double firstValue = values.get(0);
        double lastValue = values.get(values.size() - 1);
        double totalChange = lastValue - firstValue;

        // 计算变化率（百分比）
        double changeRate = totalChange / firstValue * 100;

        // 计算数据的平均值和标准差
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0);
        double stdDev = Math.sqrt(variance);

        // 计算置信度：基于标准差与平均值的比值
        // 标准差越小，说明数据越稳定，置信度越高
        double confidence = Math.min(100.0, (1.0 - (stdDev / mean)) * 100.0);
        confidence = Math.max(0.0, confidence);

        // 根据变化率判断趋势类型
        // 变化率阈值为5%，可根据实际需求调整
        TrendType trendType;
        if (Math.abs(changeRate) < 5.0) {
            trendType = TrendType.STABLE;
        } else if (changeRate > 0) {
            trendType = TrendType.RISING;
        } else {
            trendType = TrendType.FALLING;
        }

        // 组装分析结果
        result.put("trend", trendType.getDescription());
        result.put("confidence", Math.round(confidence * 100.0) / 100.0); // 保留两位小数
        result.put("changeRate", Math.round(changeRate * 100.0) / 100.0); // 保留两位小数

        return result;
    }

    /**
     * 分析水质数据趋势
     * 包括：pH值、溶解氧、氨氮、水温、电导率等指标
     * @return 包含各项水质指标趋势分析结果的Map
     */
    public Map<String, Object> analyzeWaterQualityTrends() {
        // 获取最近24小时的时间范围
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(24);

        // 构建查询条件
        LambdaQueryWrapper<AgricultureWaterQualityData> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(AgricultureWaterQualityData::getCollectTime, startTime, endTime)
                .orderByAsc(AgricultureWaterQualityData::getCollectTime);

        List<AgricultureWaterQualityData> data = waterQualityDataService.list(wrapper);
        Map<String, Object> trends = new HashMap<>();

        // 分析各个水质指标的趋势
        trends.put("ph", analyzeTrend(data, AgricultureWaterQualityData::getPhValue));
        trends.put("dissolvedOxygen", analyzeTrend(data, AgricultureWaterQualityData::getDissolvedOxygen));
        trends.put("ammoniaNitrogen", analyzeTrend(data, AgricultureWaterQualityData::getAmmoniaNitrogen));
        trends.put("waterTemperature", analyzeTrend(data, AgricultureWaterQualityData::getWaterTemperature));
        trends.put("conductivity", analyzeTrend(data, AgricultureWaterQualityData::getConductivity));

        return trends;
    }

    /**
     * 分析气象数据趋势
     * 包括：温度、湿度、风速、光照强度、降雨量、气压等指标
     * @return 包含各项气象指标趋势分析结果的Map
     */
    public Map<String, Object> analyzeWeatherTrends() {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(24);

        LambdaQueryWrapper<AgricultureWeatherData> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(AgricultureWeatherData::getCollectTime, startTime, endTime)
                .orderByAsc(AgricultureWeatherData::getCollectTime);

        List<AgricultureWeatherData> data = weatherDataService.list(wrapper);
        Map<String, Object> trends = new HashMap<>();

        // 分析各个气象指标的趋势
        trends.put("temperature", analyzeTrend(data, AgricultureWeatherData::getTemperature));
        trends.put("humidity", analyzeTrend(data, AgricultureWeatherData::getHumidity));
        trends.put("windSpeed", analyzeTrend(data, AgricultureWeatherData::getWindSpeed));
        trends.put("lightIntensity", analyzeTrend(data, AgricultureWeatherData::getLightIntensity));
        trends.put("rainfall", analyzeTrend(data, AgricultureWeatherData::getRainfall));
        trends.put("airPressure", analyzeTrend(data, AgricultureWeatherData::getAirPressure));

        return trends;
    }
}