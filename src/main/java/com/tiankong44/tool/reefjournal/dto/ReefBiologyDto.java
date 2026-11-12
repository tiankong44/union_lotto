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
    private String currentTankSince;
    private String bodyLengthCm;
    private String source;
    private String status;
    private String statusKey;
    private String statusLabel;
    /** 个体或批次的当前数量及补充资料。 */
    private String trackingMode;
    private Integer quantity;
    private String positionNote;
    private String note;
    private Integer daysInTank;
}
