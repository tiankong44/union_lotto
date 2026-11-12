package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

/**
 * 备份恢复结果，重复提交同一备份时标识已恢复。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
@Data
public class ReefBackupRestoreResult {
    private String backupId;
    private boolean alreadyRestored;
}
