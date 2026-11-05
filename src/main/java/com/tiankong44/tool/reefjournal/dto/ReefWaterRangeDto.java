package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 鱼缸单项水质范围响应。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
public class ReefWaterRangeDto {
    private String metric;
    private String label;
    private String unit;
    private BigDecimal minimum;
    private BigDecimal maximum;
}
