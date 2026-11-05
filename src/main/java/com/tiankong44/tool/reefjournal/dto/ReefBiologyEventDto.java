package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

/**
 * 生物事件时间轴响应。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
public class ReefBiologyEventDto {
    private String id;
    private String eventAt;
    private String title;
    private String description;
    private String status;
    private String statusKey;
}
