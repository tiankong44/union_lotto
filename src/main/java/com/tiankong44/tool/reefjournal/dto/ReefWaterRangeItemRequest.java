package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户提交的单项水质上下限。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
public class ReefWaterRangeItemRequest {
    private String metric;
    private BigDecimal minimum;
    private BigDecimal maximum;
}
