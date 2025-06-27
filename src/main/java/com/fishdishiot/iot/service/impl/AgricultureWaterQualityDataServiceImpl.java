package com.fishdishiot.iot.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fishdishiot.iot.domain.AgricultureWaterQualityData;
import com.fishdishiot.iot.mapper.AgricultureWaterQualityDataMapper;
import com.fishdishiot.iot.service.AgricultureWaterQualityDataService;
import org.springframework.stereotype.Service;

@Service
public class AgricultureWaterQualityDataServiceImpl
        extends ServiceImpl<AgricultureWaterQualityDataMapper, AgricultureWaterQualityData>
        implements AgricultureWaterQualityDataService {

}