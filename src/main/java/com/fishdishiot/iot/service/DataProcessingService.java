package com.fishdishiot.iot.service;

import java.util.Map;

public interface DataProcessingService {
//    /**
//     * 处理字节数组形式的数据。
//     * 假设字节数组内容为JSON字符串。
//     * @param data 原始字节数组数据
//     */
//    void processAndStore(byte[] data);

    /**
     * 处理Map类型的数据，这是由SensorCommunicationService调用的主要方法。
     * @param parsedData 从传感器通信服务传递过来的，已解析的数据Map
     */
    void processAndStore(Map<String, Object> parsedData);
}