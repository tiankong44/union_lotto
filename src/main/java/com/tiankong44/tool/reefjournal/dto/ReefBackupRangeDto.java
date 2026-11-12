package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 导出时保留水质范围与所属鱼缸的关系。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
@Data
public class ReefBackupRangeDto {
    private String id;
    private String tankId;
    private String metric;
    private BigDecimal minimum;
    private BigDecimal maximum;
}
