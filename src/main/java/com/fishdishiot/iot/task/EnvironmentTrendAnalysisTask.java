package com.fishdishiot.iot.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fishdishiot.iot.gateway.MqttGateway;
import com.fishdishiot.iot.service.EnvironmentTrendAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class EnvironmentTrendAnalysisTask {

    @Autowired
    private EnvironmentTrendAnalysisService environmentTrendAnalysisService;

    @Autowired
    private MqttGateway mqttGateway;

    @Autowired
    private ObjectMapper objectMapper;

     /**
    * 定时执行趋势分析任务
    * cron表达式说明：秒 分 时 日 月 周
    * "0 0 * * * ?" 表示每小时整点执行一次
    */
//    @Scheduled(cron = "0 0 * * * ?")  //每小时
    @Scheduled(cron = "0 * * * * ?") //测试用 1分钟一次
    public void executeAnalysis() {
        try {
            log.info("开始执行环境趋势分析任务...");

            // 分析水质数据趋势并发送
            analyzeAndPublishWaterQualityTrends();

            // 分析气象数据趋势并发送
            analyzeAndPublishWeatherTrends();

        } catch (Exception e) {
            log.error("执行环境趋势分析任务时发生错误", e);
        }
    }

    /**
     * 分析并发布水质趋势数据
     */
    private void analyzeAndPublishWaterQualityTrends() {
        try {
            Map<String, Object> waterQualityTrends = environmentTrendAnalysisService.analyzeWaterQualityTrends();
            if (waterQualityTrends != null && !waterQualityTrends.isEmpty()) {
                Map<String, Object> message = new HashMap<>();
                message.put("type", "waterQuality");
                message.put("data", waterQualityTrends);
                message.put("analysisTime", LocalDateTime.now().toString());
                message.put("deviceInfo", environmentTrendAnalysisService.getWaterQualityDeviceInfo());

                String topic = "/fish-dish/trends/water-quality";
                mqttGateway.sendToMqtt(objectMapper.writeValueAsString(message), topic);
                log.info("水质趋势分析数据已发送到MQTT主题: {}", topic);
            } else {
                log.warn("未获取到水质趋势数据");
            }
        } catch (Exception e) {
            log.error("分析并发布水质趋势数据时发生错误", e);
        }
    }

    /**
     * 分析并发布气象趋势数据
     */
    private void analyzeAndPublishWeatherTrends() {
        try {
            Map<String, Object> weatherTrends = environmentTrendAnalysisService.analyzeWeatherTrends();
            if (weatherTrends != null && !weatherTrends.isEmpty()) {
                Map<String, Object> message = new HashMap<>();
                message.put("type", "weather");
                message.put("data", weatherTrends);
                message.put("analysisTime", LocalDateTime.now().toString());
                message.put("deviceInfo", environmentTrendAnalysisService.getWeatherDeviceInfo());

                String topic = "/fish-dish/trends/weather";
                mqttGateway.sendToMqtt(objectMapper.writeValueAsString(message), topic);
                log.info("气象趋势分析数据已发送到MQTT主题: {}", topic);
            } else {
                log.warn("未获取到气象趋势数据");
            }
        } catch (Exception e) {
            log.error("分析并发布气象趋势数据时发生错误", e);
        }
    }
}