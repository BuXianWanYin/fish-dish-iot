package com.fishdishiot.iot.service.impl;

import com.fishdishiot.iot.domain.AgricultureDevice;
import com.fishdishiot.iot.service.AgricultureDeviceService;
import com.fishdishiot.iot.service.AgricultureDeviceStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 设备状态管理Service业务层处理
 *
 * @author bxwy
 */
@Service
public class AgricultureDeviceStatusServiceImpl implements AgricultureDeviceStatusService {

    @Autowired
    private AgricultureDeviceService agricultureDeviceService;

    private static final Logger log = LoggerFactory.getLogger(AgricultureDeviceService.class);

    // 在线
    @Override
    public void updateDeviceOnline(String deviceId) {
        Long id = Long.parseLong(deviceId);
        // 查询设备详情
        AgricultureDevice device = agricultureDeviceService.getById(id);
        if (device != null) {
            AgricultureDevice deviceToUpdate = new AgricultureDevice();
            deviceToUpdate.setId(id);
            deviceToUpdate.setStatus("1");
            deviceToUpdate.setLastOnlineTime(new Date());
            agricultureDeviceService.updateById(deviceToUpdate);

            //只有状态变化时才打印上线日志
            if (!"1".equals(device.getStatus())) {
                log.info("设备 {} ({}) 上线, 当前时间: {}", device.getId(), device.getDeviceName(), new Date());
            }
        }
    }

    // 离线
    @Override
    public void updateDeviceOffline(String deviceId) {
        AgricultureDevice deviceToUpdate = new AgricultureDevice();
        deviceToUpdate.setId(Long.parseLong(deviceId));
        // 设置物理在线状态为离线
        deviceToUpdate.setStatus("0");
        // 设置用户控制状态为关闭
        deviceToUpdate.setControlStatus("0");
        agricultureDeviceService.updateById(deviceToUpdate);
    }
}