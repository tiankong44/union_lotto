package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 单项水质测量值，单位和方法随用户记录保存。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
@Data
public class ReefWaterMeasurementDto {
    @NotBlank(message = "请选择水质指标")
    private String metric;
    @NotBlank(message = "请填写水质测量值")
    private String value;
    @Size(max = 32, message = "测量单位不能超过32个字符")
    private String unit;
    @Size(max = 128, message = "测量方法不能超过128个字符")
    private String method;
}
