package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

/**
 * 备份时批量读取日志与生物的关联。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
@Data
public class ReefBackupLogBiologyDto {
    private String logId;
    private String biologyId;
}
