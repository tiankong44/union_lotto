package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 设备新增或更新请求。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
public class ReefEquipmentSaveRequest {
    private String id;
    private String tankId;
    @NotBlank(message = "设备分类不能为空")
    private String category;
    @NotBlank(message = "设备名称不能为空")
    @Size(max = 128, message = "设备名称不能超过128个字符")
    private String name;
    @Size(max = 128, message = "设备型号不能超过128个字符")
    private String model;
    private String status;
    private BigDecimal powerWatt;
    private BigDecimal dailyHours;
    private String startedAt;
    private Integer maintenanceIntervalDays;
    private String lastMaintainedAt;
}
