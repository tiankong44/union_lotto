package com.tiankong44.tool.reefjournal.controller;

import com.tiankong44.tool.base.entity.BaseRes;
import com.tiankong44.tool.reefjournal.ReefJournalConstants;
import com.tiankong44.tool.reefjournal.dto.ReefBackupBatchRequest;
import com.tiankong44.tool.reefjournal.dto.ReefBackupPhotoRequest;
import com.tiankong44.tool.reefjournal.dto.ReefBackupRestoreResult;
import com.tiankong44.tool.reefjournal.service.ReefBackupPhotoImportService;
import com.tiankong44.tool.reefjournal.service.ReefBackupRestoreService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.validation.Valid;

/**
 * 当前用户备份的分批恢复入口。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
@RestController
@RequestMapping("/reef-journal/backup")
public class ReefBackupController {
    private final ReefBackupRestoreService restoreService;
    private final ReefBackupPhotoImportService photoService;

    /** 创建备份恢复控制器。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    public ReefBackupController(ReefBackupRestoreService restoreService, ReefBackupPhotoImportService photoService) {
        this.restoreService = restoreService; this.photoService = photoService;
    }

    /** 为一次恢复生成随机编号，便于失败重试和再次创建副本。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    @PostMapping("/session")
    public BaseRes<String> createSession(@RequestAttribute(ReefJournalConstants.REQUEST_USER_ID) Long userId) {
        return BaseRes.success(java.util.UUID.randomUUID().toString());
    }

    /** 恢复一批结构化记录。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    @PostMapping("/restore")
    public BaseRes<ReefBackupRestoreResult> restore(@RequestAttribute(ReefJournalConstants.REQUEST_USER_ID) Long userId,
                                                    @RequestBody @Valid ReefBackupBatchRequest request) {
        return BaseRes.success(restoreService.restore(userId, request));
    }

    /** 逐张恢复备份照片并返回新照片编号。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    @PostMapping(value = "/{backupId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseRes<String> restorePhoto(@RequestAttribute(ReefJournalConstants.REQUEST_USER_ID) Long userId,
                                        @PathVariable String backupId, @ModelAttribute @Valid ReefBackupPhotoRequest request,
                                        @RequestParam("file") MultipartFile file) {
        return BaseRes.success(photoService.restore(userId, backupId, request, file));
    }
}
