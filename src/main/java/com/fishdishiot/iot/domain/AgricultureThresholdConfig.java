package com.fishdishiot.iot.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


/** 
 * @author bxwy
 * @description  
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value ="agriculture_threshold_config")
@ApiModel(value = "AgricultureThresholdConfig" , description="阈值配置表")
public class AgricultureThresholdConfig extends BaseEntityPlus implements Serializable  {

    private static final long serialVersionUID = 1L;

    @TableId(value="id",type = IdType.AUTO)
    @ApiModelProperty(value="主键ID")
    private Long id;

    @TableField(value="device_id")
    @ApiModelProperty(value="设备ID")
    private Long deviceId;

    @TableField(value="device_type")
    @ApiModelProperty(value="设备类型（气象、水质等）")
    private String deviceType;

    @TableField(value="param_type")
    @ApiModelProperty(value="参数类型（如：风速、温度、PH值等）")
    private String paramType;

    @TableField(value="unit")
    @ApiModelProperty(value="单位")
    private String unit;

    @TableField(value="threshold_min")
    @ApiModelProperty(value="阈值最小值")
    private Double thresholdMin;

    @TableField(value="threshold_max")
    @ApiModelProperty(value="阈值最大值")
    private Double thresholdMax;

    @TableField(value="is_enabled")
    @ApiModelProperty(value="是否启用（0-禁用，1-启用）")
    private Long isEnabled;

}
