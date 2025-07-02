package com.fishdishiot.iot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fishdishiot.iot.domain.AgricultureWaterQualityData;
import com.fishdishiot.iot.domain.AjaxResult;

public interface DeviceOperationService {
    AjaxResult controlDevice(Long deviceId, String action, Integer index);
}
