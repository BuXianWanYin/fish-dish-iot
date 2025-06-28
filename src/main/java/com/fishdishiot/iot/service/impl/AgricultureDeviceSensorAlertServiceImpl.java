package com.fishdishiot.iot.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fishdishiot.iot.domain.AgricultureThresholdConfig;
import com.fishdishiot.iot.gateway.MqttGateway;
import com.fishdishiot.iot.service.AgricultureThresholdConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fishdishiot.iot.mapper.AgricultureDeviceSensorAlertMapper;
import com.fishdishiot.iot.domain.AgricultureDeviceSensorAlert;
import com.fishdishiot.iot.service.AgricultureDeviceSensorAlertService;

/**
 * 传感器预警信息Service业务层处理
 * 
 * @author server
 * @date 2025-05-26
 */
@Service
public class AgricultureDeviceSensorAlertServiceImpl extends ServiceImpl<AgricultureDeviceSensorAlertMapper, AgricultureDeviceSensorAlert> implements AgricultureDeviceSensorAlertService
{
    private static final Logger log = LoggerFactory.getLogger(AgricultureDeviceSensorAlertServiceImpl.class);

    @Autowired
    private AgricultureThresholdConfigService thresholdConfigService;

    @Autowired
    private MqttGateway mqttGateway;

    @Autowired
    private ObjectMapper objectMapper;

       /**
     * 检查传感器数据是否超出阈值并生成预警（含去重 自动消警）。
     * 1. 若数据超出阈值，先查找最近一条同类预警（同设备、参数、类型、分区、大棚）。
     *    - 若存在未恢复（status=0）的同类预警，则不再生成新预警，避免重复。
     *    - 若无未恢复预警，则插入新预警。
     * 2. 若数据恢复正常，且存在未恢复的同类预警，自动将其状态设为已处理（status=1）。
     * 该方法支持人工消警和自动消警，保证同一异常只生成一条预警，恢复后再异常才生成新预警。
     *
     * @param deviceId   设备ID
     * @param deviceName 设备名称
     * @param deviceType 设备类型
     * @param pastureId  大棚ID
     * @param batchId    分区ID
     * @param paramType  参数类型
     * @param paramValue 参数值
     * @param unit       单位
     */
    @Override
    public void checkAndGenerateAlert(Long deviceId, String deviceName, String deviceType,
                                      String pastureId, String batchId, String paramType,
                                      Double paramValue, String unit) {
        try {
            // 1. 获取阈值配置
            AgricultureThresholdConfig config = thresholdConfigService.getConfigByDeviceIdAndParamType(deviceId, paramType);
            if (config == null) {
                log.debug("设备 {} 参数 {} 未配置阈值，跳过预警检查", deviceId, paramType);
                return;
            }
    
            // 2. 检查是否超出阈值
            boolean isAlert = false;
            String alertType = "";
            String alertMessage = "";
            Long alertLevel = 0L; // 默认警告级别
    
            BigDecimal minThreshold = config.getThresholdMin() == null ? null : BigDecimal.valueOf(config.getThresholdMin());
            BigDecimal maxThreshold = config.getThresholdMax() == null ? null : BigDecimal.valueOf(config.getThresholdMax());
            BigDecimal value = BigDecimal.valueOf(paramValue);
    
            // 检查下限
            if (minThreshold != null && value.compareTo(minThreshold) < 0) {
                isAlert = true;
                alertType = "LOW";
                alertLevel = determineAlertLevel(value, minThreshold, true);
                alertMessage = String.format("%s值过低: %.2f%s，低于阈值%.2f%s",
                        paramType, paramValue, unit, minThreshold, unit);
            }
            // 检查上限
            else if (maxThreshold != null && value.compareTo(maxThreshold) > 0) {
                isAlert = true;
                alertType = "HIGH";
                alertLevel = determineAlertLevel(value, maxThreshold, false);
                alertMessage = String.format("%s值过高: %.2f%s，超过阈值%.2f%s",
                        paramType, paramValue, unit, maxThreshold, unit);
            }
    
            // 3. 预警去重与自动消警
            AgricultureDeviceSensorAlert lastAlert = this.getOne(
                new QueryWrapper<AgricultureDeviceSensorAlert>()
                    .eq("device_id", deviceId)
                    .eq("param_name", paramType)
                    .eq("alert_type", alertType)
                    .eq("batch_id", batchId)
                    .eq("pasture_id", pastureId)
                    .orderByDesc("alert_time")
                    .last("limit 1")
            );
    
            if (isAlert) {
                // 如果有同类预警且未恢复（无论人工还是自动消警），都不生成新预警
                if (lastAlert != null && lastAlert.getStatus() != null && lastAlert.getStatus() == 0L) {
                    log.info("已有未恢复的相同预警，跳过生成: {}", alertMessage);
                    return;
                }
                // 生成新预警
                AgricultureDeviceSensorAlert alert = AgricultureDeviceSensorAlert.builder()
                        .alertType(alertType)
                        .alertMessage(alertMessage)
                        .paramName(paramType)
                        .paramValue(String.valueOf(paramValue))
                        .thresholdMin(minThreshold != null ? minThreshold.doubleValue() : null)
                        .thresholdMax(maxThreshold != null ? maxThreshold.doubleValue() : null)
                        .pastureId(pastureId)
                        .batchId(batchId)
                        .deviceId(deviceId)
                        .deviceName(deviceName)
                        .deviceType(deviceType)
                        .alertTime(LocalDateTime.now())
                        .alertLevel(alertLevel)
                        .status(0L) // 未处理
                        .build();
    
                this.insertAgricultureDeviceSensorAlert(alert);
                log.warn("生成预警: {}", alertMessage);
                processAlert(alert);
            } else {
                // 数据已恢复，自动消警
                if (lastAlert != null && lastAlert.getStatus() != null && lastAlert.getStatus() == 0L) {
                    lastAlert.setStatus(1L);
                    lastAlert.setUpdateTime(LocalDateTime.now());
                    this.updateById(lastAlert);
                    log.info("数据恢复，自动消警: deviceId={}, paramType={}, alertType={}", deviceId, paramType, lastAlert.getAlertType());
                }
            }
        } catch (Exception e) {
            log.error("检查预警时发生错误: deviceId={}, paramType={}, paramValue={}",
                    deviceId, paramType, paramValue, e);
        }
    }


     /**
     * 处理预警信息（推送MQTT消息）。
     * 构建预警消息内容（包含预警ID、设备信息、参数、级别、时间等）。
     * 将预警消息以JSON格式推送到指定MQTT主题。
     *
     * @param alert 预警信息对象
     */
    @Override
    public void processAlert(AgricultureDeviceSensorAlert alert) {
        try {
            // 1. 构建MQTT消息
            Map<String, Object> alertMessage = new HashMap<>();
            alertMessage.put("alertId", alert.getId());
            alertMessage.put("deviceId", alert.getDeviceId());
            alertMessage.put("deviceName", alert.getDeviceName());
            alertMessage.put("alertType", alert.getAlertType());
            alertMessage.put("alertMessage", alert.getAlertMessage());
            alertMessage.put("paramName", alert.getParamName());
            alertMessage.put("paramValue", alert.getParamValue());
            alertMessage.put("alertLevel", alert.getAlertLevel());
            alertMessage.put("alertTime", alert.getAlertTime());
            alertMessage.put("pastureId", alert.getPastureId());
            alertMessage.put("batchId", alert.getBatchId());

            // 2. 发送到MQTT主题
            String topic = "/fish-dish/alerts";
            String payload = objectMapper.writeValueAsString(alertMessage);

            mqttGateway.sendToMqtt(payload, topic, 1);
            log.info("预警消息已发送到MQTT主题 {}: {}", topic, payload);

        } catch (Exception e) {
            log.error("处理预警消息时发生错误: alertId={}", alert.getId(), e);
        }
    }

    /**
     * 根据偏离程度确定预警级别
     * @param value 当前值
     * @param threshold 阈值
     * @param isLow 是否为下限检查
     * @return 预警级别 (0-警告, 1-严重)
     */
    private Long determineAlertLevel(BigDecimal value, BigDecimal threshold, boolean isLow) {
        BigDecimal deviation;
        if (isLow) {
            deviation = threshold.subtract(value).divide(threshold, 4, BigDecimal.ROUND_HALF_UP);
        } else {
            deviation = value.subtract(threshold).divide(threshold, 4, BigDecimal.ROUND_HALF_UP);
        }

        // 偏离超过50%为严重级别，否则为警告级别
        return deviation.compareTo(BigDecimal.valueOf(0.5)) > 0 ? 1L : 0L;
    }


    @Autowired
    private AgricultureDeviceSensorAlertMapper agricultureDeviceSensorAlertMapper;

    /**
     * 查询传感器预警信息
     *
     * @param id 传感器预警信息主键
     * @return 传感器预警信息
     */
    @Override
    public AgricultureDeviceSensorAlert selectAgricultureDeviceSensorAlertById(Long id)
    {
        return getById(id);
    }

    /**
     * 查询传感器预警信息列表
     *
     * @param agricultureDeviceSensorAlert 传感器预警信息
     * @return 传感器预警信息
     */
    @Override
    public List<AgricultureDeviceSensorAlert> selectAgricultureDeviceSensorAlertList(AgricultureDeviceSensorAlert agricultureDeviceSensorAlert)
    {
        return list();
    }

    /**
     * 新增传感器预警信息
     *
     * @param agricultureDeviceSensorAlert 传感器预警信息
     * @return 结果
     */
    @Override
    public int insertAgricultureDeviceSensorAlert(AgricultureDeviceSensorAlert agricultureDeviceSensorAlert)
    {
        agricultureDeviceSensorAlert.setCreateTime(LocalDateTime.now());
        return agricultureDeviceSensorAlertMapper.insert(agricultureDeviceSensorAlert);
    }

    /**
     * 修改传感器预警信息
     *
     * @param agricultureDeviceSensorAlert 传感器预警信息
     * @return 结果
     */
    @Override
    public int updateAgricultureDeviceSensorAlert(AgricultureDeviceSensorAlert agricultureDeviceSensorAlert)
    {
        agricultureDeviceSensorAlert.setUpdateTime(LocalDateTime.now());
        return agricultureDeviceSensorAlertMapper.updateById(agricultureDeviceSensorAlert);
    }

    /**
     * 批量删除传感器预警信息
     *
     * @param ids 需要删除的传感器预警信息主键
     * @return 结果
     */
    @Override
    public int deleteAgricultureDeviceSensorAlertByIds(Long[] ids)
    {
        return removeByIds(Arrays.asList(ids)) ? ids.length : 0;
    }

    /**
     * 删除传感器预警信息信息
     *
     * @param id 传感器预警信息主键
     * @return 结果
     */
    @Override
    public int deleteAgricultureDeviceSensorAlertById(Long id)
    {
        return removeById(id) ? 1 : 0;
    }
}
