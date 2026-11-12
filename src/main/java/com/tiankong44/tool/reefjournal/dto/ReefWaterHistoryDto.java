package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

/**
 * 查询水质历史时使用的指标和测量时间。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
@Data
public class ReefWaterHistoryDto {
    private String metric;
    private String recordedAt;
    private String value;
}
