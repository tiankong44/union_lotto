package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

/**
 * 备份原编号到当前账号新编号的映射。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
@Data
public class ReefBackupMappingDto {
    private String entityType;
    private String originalId;
    private String restoredId;
}
