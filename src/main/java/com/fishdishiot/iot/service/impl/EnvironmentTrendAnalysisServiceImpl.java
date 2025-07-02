package com.fishdishiot.iot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fishdishiot.iot.domain.AgricultureWaterQualityData;
import com.fishdishiot.iot.domain.AgricultureWeatherData;
import com.fishdishiot.iot.service.AgricultureWaterQualityDataService;
import com.fishdishiot.iot.service.AgricultureWeatherDataService;
import com.fishdishiot.iot.service.EnvironmentTrendAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

@Slf4j
@Service
public class EnvironmentTrendAnalysisServiceImpl implements EnvironmentTrendAnalysisService {

    @Autowired
    private AgricultureWaterQualityDataService waterQualityDataService;

    @Autowired
    private AgricultureWeatherDataService weatherDataService;

    /**
     * 获取最近24小时内最新一条水质监测设备信息
     * @return 包含 deviceId、pastureId、batchId 的设备信息Map
     */
    @Override
    public Map<String, Object> getWaterQualityDeviceInfo() {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(24);

        LambdaQueryWrapper<AgricultureWaterQualityData> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(AgricultureWaterQualityData::getCollectTime, startTime, endTime)
                .orderByDesc(AgricultureWaterQualityData::getCollectTime)
                .last("LIMIT 1");

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
     * 获取最近24小时内最新一条气象监测设备信息
     * @return 包含 deviceId、pastureId、batchId 的设备信息Map
     */
    @Override
    public Map<String, Object> getWeatherDeviceInfo() {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(24);

        LambdaQueryWrapper<AgricultureWeatherData> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(AgricultureWeatherData::getCollectTime, startTime, endTime)
                .orderByDesc(AgricultureWeatherData::getCollectTime)
                .last("LIMIT 1");

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
    private <T> Map<String, Object> analyzeTrend(List<T> data, Function<T, Double> valueExtractor) {
        Map<String, Object> result = new HashMap<>();

        if (data == null || data.isEmpty()) {
            result.put("trend", TrendType.STABLE.getDescription());
            result.put("confidence", 0.0);
            return result;
        }

        List<Double> values = new ArrayList<>();
        for (T item : data) {
            Double value = valueExtractor.apply(item);
            if (value != null) {
                values.add(value);
            }
        }

        if (values.size() < 2) {
            result.put("trend", TrendType.STABLE.getDescription());
            result.put("confidence", 0.0);
            return result;
        }

        double firstValue = values.get(0);
        double lastValue = values.get(values.size() - 1);
        double totalChange = lastValue - firstValue;
        double changeRate = totalChange / firstValue * 100;

        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0);
        double stdDev = Math.sqrt(variance);

        double confidence = Math.min(100.0, (1.0 - (stdDev / mean)) * 100.0);
        confidence = Math.max(0.0, confidence);

        TrendType trendType;
        if (Math.abs(changeRate) < 5.0) {
            trendType = TrendType.STABLE;
        } else if (changeRate > 0) {
            trendType = TrendType.RISING;
        } else {
            trendType = TrendType.FALLING;
        }

        result.put("trend", trendType.getDescription());
        result.put("confidence", Math.round(confidence * 100.0) / 100.0);
        result.put("changeRate", Math.round(changeRate * 100.0) / 100.0);

        return result;
    }

    /**
     * 分析最近24小时内水质数据的各项指标趋势
     * 包括：pH值、溶解氧、氨氮、水温、电导率
     * @return 包含各项水质指标趋势分析结果的Map
     */
    @Override
    public Map<String, Object> analyzeWaterQualityTrends() {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(24);

        LambdaQueryWrapper<AgricultureWaterQualityData> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(AgricultureWaterQualityData::getCollectTime, startTime, endTime)
                .orderByAsc(AgricultureWaterQualityData::getCollectTime);

        List<AgricultureWaterQualityData> data = waterQualityDataService.list(wrapper);
        Map<String, Object> trends = new HashMap<>();

        trends.put("ph", analyzeTrend(data, AgricultureWaterQualityData::getPhValue));
        trends.put("dissolvedOxygen", analyzeTrend(data, AgricultureWaterQualityData::getDissolvedOxygen));
        trends.put("ammoniaNitrogen", analyzeTrend(data, AgricultureWaterQualityData::getAmmoniaNitrogen));
        trends.put("waterTemperature", analyzeTrend(data, AgricultureWaterQualityData::getWaterTemperature));
        trends.put("conductivity", analyzeTrend(data, AgricultureWaterQualityData::getConductivity));

        return trends;
    }

    /**
     * 分析最近24小时内气象数据的各项指标趋势
     * 包括：温度、湿度、风速、光照强度、降雨量、气压
     * @return 包含各项气象指标趋势分析结果的Map
     */
    @Override
    public Map<String, Object> analyzeWeatherTrends() {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(24);

        LambdaQueryWrapper<AgricultureWeatherData> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(AgricultureWeatherData::getCollectTime, startTime, endTime)
                .orderByAsc(AgricultureWeatherData::getCollectTime);

        List<AgricultureWeatherData> data = weatherDataService.list(wrapper);
        Map<String, Object> trends = new HashMap<>();

        trends.put("temperature", analyzeTrend(data, AgricultureWeatherData::getTemperature));
        trends.put("humidity", analyzeTrend(data, AgricultureWeatherData::getHumidity));
        trends.put("windSpeed", analyzeTrend(data, AgricultureWeatherData::getWindSpeed));
        trends.put("lightIntensity", analyzeTrend(data, AgricultureWeatherData::getLightIntensity));
        trends.put("rainfall", analyzeTrend(data, AgricultureWeatherData::getRainfall));
        trends.put("airPressure", analyzeTrend(data, AgricultureWeatherData::getAirPressure));

        return trends;
    }
}