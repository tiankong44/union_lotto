package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

/**
 * 调度器读取的设备维护周期信息。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
public class ReefReminderEquipmentDto {
    private Long id;
    private Long userId;
    private String name;
    private String lastMaintainedAt;
    private Integer maintenanceIntervalDays;
}
