package com.fishdishiot.iot.domain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 设备操作请求体 DTO
 */
@Data
@ApiModel(value = "DeviceControlRequest", description = "设备操作请求体")
public class DeviceControlRequest implements Serializable {

    @ApiModelProperty(value = "设备ID", required = true)
    private Long deviceId;

    @ApiModelProperty(value = "操作类型（on=开启，off=关闭）", required = true)
    private String action;

    @ApiModelProperty(value = "指令索引（0=单组，1=多组）", required = false, example = "0")
    private Integer index = 0; // 默认0
}