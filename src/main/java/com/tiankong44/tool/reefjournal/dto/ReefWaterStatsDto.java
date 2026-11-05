package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

/**
 * 水质趋势统计摘要。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
public class ReefWaterStatsDto {
    private String currentValue;
    private String average7Days;
    private String trendKey;
    private String trendLabel;
}
