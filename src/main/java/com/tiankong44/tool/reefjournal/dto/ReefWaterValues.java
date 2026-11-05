package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 日志中的水质检测数值。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
public class ReefWaterValues {
    private BigDecimal salinity;
    private BigDecimal temperature;
    private BigDecimal ph;
    private BigDecimal no2;
    private BigDecimal no3;
    private BigDecimal po4;
}
