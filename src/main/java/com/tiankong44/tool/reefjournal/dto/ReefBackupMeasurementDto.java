package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 批量导出时携带日志编号的水质测量值。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReefBackupMeasurementDto extends ReefWaterMeasurementDto {
    private String logId;
}
