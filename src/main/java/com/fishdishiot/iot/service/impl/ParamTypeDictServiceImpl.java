package com.fishdishiot.iot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fishdishiot.iot.domain.ParamTypeDict;
import com.fishdishiot.iot.mapper.ParamTypeDictMapper;
import com.fishdishiot.iot.service.ParamTypeDictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 传感器参数类型中英文对照Service业务层处理
 * 
 * @author server
 * @date 2025-06-28
 */
@Service
public class ParamTypeDictServiceImpl extends ServiceImpl<ParamTypeDictMapper, ParamTypeDict> implements ParamTypeDictService
{
    @Autowired
    private ParamTypeDictMapper paramTypeDictMapper;

    /**
     * 查询传感器参数类型中英文对照
     * 
     * @param id 传感器参数类型中英文对照主键
     * @return 传感器参数类型中英文对照
     */
    @Override
    public ParamTypeDict selectParamTypeDictById(Long id)
    {
        return getById(id);
    }

    /**
     * 查询传感器参数类型中英文对照列表
     * 
     * @param paramTypeDict 传感器参数类型中英文对照
     * @return 传感器参数类型中英文对照
     */
    @Override
    public List<ParamTypeDict> selectParamTypeDictList(ParamTypeDict paramTypeDict)
    {
        return list();
    }

    /**
     * 新增传感器参数类型中英文对照
     * 
     * @param paramTypeDict 传感器参数类型中英文对照
     * @return 结果
     */
    @Override
    public int insertParamTypeDict(ParamTypeDict paramTypeDict)
    {
        return paramTypeDictMapper.insert(paramTypeDict);
    }

    /**
     * 修改传感器参数类型中英文对照
     * 
     * @param paramTypeDict 传感器参数类型中英文对照
     * @return 结果
     */
    @Override
    public int updateParamTypeDict(ParamTypeDict paramTypeDict)
    {
        return updateById(paramTypeDict) ? 1 : 0;
    }

    /**
     * 批量删除传感器参数类型中英文对照
     * 
     * @param ids 需要删除的传感器参数类型中英文对照主键
     * @return 结果
     */
    @Override
    public int deleteParamTypeDictByIds(Long[] ids)
    {
        return removeByIds(Arrays.asList(ids)) ? ids.length : 0;
    }

    /**
     * 删除传感器参数类型中英文对照信息
     * 
     * @param id 传感器参数类型中英文对照主键
     * @return 结果
     */
    @Override
    public int deleteParamTypeDictById(Long id)
    {
        return removeById(id) ? 1 : 0;
    }

    @Override
    public String getCnNameByEn(String en) {
        ParamTypeDict dict = this.getOne(new QueryWrapper<ParamTypeDict>().eq("param_type_en", en));
        return dict != null ? dict.getParamTypeCn() : en;
    }
}
