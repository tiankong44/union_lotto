package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

/**
 * 设备列表响应。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
public class ReefEquipmentDto {
    private String id;
    private String tankId;
    private String category;
    private String name;
    private String model;
    private String status;
    private String statusLabel;
    private String dailyHours;
    private String startedAt;
    private Integer daysRunning;
    private Integer maintenanceIntervalDays;
    private String lastMaintainedAt;
}
