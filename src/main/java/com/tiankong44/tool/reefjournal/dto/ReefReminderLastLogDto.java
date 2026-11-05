package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

/**
 * 按用户和日志类型聚合得到的最近记录时间。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
public class ReefReminderLastLogDto {
    private Long userId;
    private String type;
    private String lastAt;
}
