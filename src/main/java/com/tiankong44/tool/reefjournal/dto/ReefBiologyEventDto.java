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
    /** 导出时保留事件与所属生物、发生鱼缸之间的关系。 */
    private String biologyId;
    private String eventType;
    private String fromTankId;
    private String toTankId;
    private String relatedBiologyId;
    private Integer quantity;
    private String eventAt;
    private String title;
    private String description;
    private String status;
    private String statusKey;
}
