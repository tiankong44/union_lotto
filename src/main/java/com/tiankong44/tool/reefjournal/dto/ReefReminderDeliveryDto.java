package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

/**
 * 已登记的单次提醒发送任务。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
public class ReefReminderDeliveryDto {
    private String taskKey;
    private Long userId;
    private String type;
    private Long entityId;
    private String dueAt;
}
