package com.tiankong44.tool.reefjournal.service;

import cn.hutool.crypto.digest.DigestUtil;
import com.tiankong44.tool.reefjournal.dto.ReefBackupPhotoRequest;
import com.tiankong44.tool.reefjournal.exception.ReefJournalException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Arrays;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.*;

/**
 * 在事务外读取和校验单张备份照片，再交给共享存储服务。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
@Service
public class ReefBackupPhotoImportService {
    private final ReefBackupPhotoStorageService storage;

    /** 创建照片恢复入口。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    public ReefBackupPhotoImportService(ReefBackupPhotoStorageService storage) { this.storage = storage; }

    /** 读取图片并计算摘要，文件读取发生在数据库事务之外。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    public String restore(Long userId, String backupId, ReefBackupPhotoRequest request, MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_PHOTO_BYTES) throw new ReefJournalException("备份照片为空或超过10 MB");
        String contentType = request.getContentType().toLowerCase(java.util.Locale.ROOT);
        if (!contentType.matches("image/[a-z0-9][a-z0-9.+-]{0,63}")) throw new ReefJournalException("备份照片类型无效");
        request.setContentType(contentType);
        if (!request.getOriginalId().matches("[1-9][0-9]{0,18}")) throw new ReefJournalException("备份照片编号无效");
        if (!Arrays.asList(PHOTO_CATEGORY_TANK, PHOTO_CATEGORY_FISH, PHOTO_CATEGORY_CORAL, PHOTO_CATEGORY_EQUIPMENT, PHOTO_CATEGORY_PLANT, PHOTO_CATEGORY_OTHER).contains(request.getCategory())) {
            throw new ReefJournalException("备份照片分类不受支持");
        }
        try {
            // 先读取文件并计算摘要，随后才进入共享存储事务。
            byte[] content = file.getBytes();
            String md5 = DigestUtil.md5Hex(content);
            return storage.store(userId, backupId, request, content, md5);
        } catch (IOException exception) {
            throw new ReefJournalException("备份照片无法读取，请重新选择备份文件");
        }
    }
}
