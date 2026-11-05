package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

/**
 * 生物详情和列表项响应。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
public class ReefBiologyDto {
    private String id;
    private String tankId;
    private String name;
    private String scientificName;
    private String category;
    private String addedAt;
    private String bodyLengthCm;
    private String source;
    private String status;
    private String statusKey;
    private String statusLabel;
    private Integer daysInTank;
}
