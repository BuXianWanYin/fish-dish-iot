package com.fishdishiot.iot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fishdishiot.iot.domain.ParamTypeDict;
import org.apache.ibatis.annotations.Mapper;

/**
 * 传感器参数类型中英文对照Mapper接口
 * 
 * @author server
 * @date 2025-06-28
 */
@Mapper
public interface ParamTypeDictMapper extends BaseMapper<ParamTypeDict>
{

}
