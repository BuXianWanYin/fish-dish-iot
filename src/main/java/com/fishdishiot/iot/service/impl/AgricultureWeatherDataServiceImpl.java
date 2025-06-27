package com.fishdishiot.iot.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fishdishiot.iot.domain.AgricultureWeatherData;
import com.fishdishiot.iot.mapper.AgricultureWeatherDataMapper;
import com.fishdishiot.iot.service.AgricultureWeatherDataService;
import org.springframework.stereotype.Service;

@Service
public class AgricultureWeatherDataServiceImpl
        extends ServiceImpl<AgricultureWeatherDataMapper, AgricultureWeatherData>
        implements AgricultureWeatherDataService {
    // 空实现即可，MyBatis-Plus 已经实现了基础的增删改查
}