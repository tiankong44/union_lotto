package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

/**
 * 调度器批量读取的用户提醒设置。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
public class ReefEnabledReminderDto {
    private Long userId;
    private String type;
    private String sendTime;
}
