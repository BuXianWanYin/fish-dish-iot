package com.fishdishiot.iot.service;

import java.util.Map;

public interface AutoControlService {
    /**
     * 自动检查所有启用的设备自动调节策略，并根据本次采集到的传感器数据自动执行设备操作。
     * @param parsedData 解析后的传感器数据Map，key为参数名（如"temperature"），value为实际采集值
     */
    void checkAndExecuteStrategy(Map<String, Object> parsedData);
}
