package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

/**
 * 单项提醒设置及可用订阅模板信息。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
public class ReefReminderItemDto {
    private String type;
    private Boolean enabled;
    private Integer availableCount;
    private String templateId;
}
