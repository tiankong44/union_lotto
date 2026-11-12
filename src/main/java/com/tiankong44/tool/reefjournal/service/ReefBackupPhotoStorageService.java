package com.tiankong44.tool.reefjournal.service;

import com.tiankong44.tool.common.entity.CloudFile;
import com.tiankong44.tool.common.mapper.CloudFileMapper;
import com.tiankong44.tool.reefjournal.dto.ReefBackupPhotoRequest;
import com.tiankong44.tool.reefjournal.exception.ReefJournalException;
import com.tiankong44.tool.reefjournal.support.ReefDateTimeSupport;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import static com.tiankong44.tool.reefjournal.ReefAquariumConstants.*;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.*;

/**
 * 将已在事务外读取的备份照片写入共享存储，并记录幂等映射。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
@Service
public class ReefBackupPhotoStorageService {
    private final JdbcTemplate jdbcTemplate;
    private final CloudFileMapper cloudFileMapper;

    /** 创建使用共享文件表和备份映射的照片恢复服务。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    public ReefBackupPhotoStorageService(JdbcTemplate jdbcTemplate, CloudFileMapper cloudFileMapper) {
        this.jdbcTemplate = jdbcTemplate; this.cloudFileMapper = cloudFileMapper;
    }

    /**
     * 在同一数据库事务中保存照片和映射，不读取本地文件。
     * @Author zhanghao_SMEICS
     * @Date 2026-11-12
     */
    @Transactional
    public String store(Long userId, String backupId, ReefBackupPhotoRequest request, byte[] content, String md5) {
        // 锁定当前用户的恢复批次，防止两台实例重复导入同一张照片。
        List<Long> batches = jdbcTemplate.query("SELECT id FROM reef_backup_restore WHERE user_id = ? AND backup_key = ? FOR UPDATE",
                (row, index) -> row.getLong(1), userId, backupId);
        if (batches.isEmpty()) throw new ReefJournalException("请先恢复备份中的鱼缸和记录");
        List<String> existing = jdbcTemplate.query("SELECT CAST(restored_id AS CHAR) FROM reef_backup_mapping WHERE user_id = ? AND backup_key = ? AND entity_type = ? AND original_id = ?",
                (row, index) -> row.getString(1), userId, backupId, BACKUP_PHOTO, request.getOriginalId());
        if (!existing.isEmpty()) return existing.get(ZERO);
        String referenceId = restoredReference(userId, backupId, request.getReferenceType(), request.getReferenceId());
        CloudFile file = new CloudFile();
        file.setFileKey(UUID.randomUUID().toString().replace("-", ""));
        // 使用固定安全名称和随机文件编号，不信任备份中的文件路径。
        file.setOriginalName("恢复的鱼缸照片"); file.setContentType(request.getContentType());
        file.setFileSize((long) content.length); file.setFileMd5(md5); file.setFileContent(content);
        file.setCreatedAt(ReefDateTimeSupport.now());
        cloudFileMapper.insert(file);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO reef_photo (user_id, file_key, category, title, reference_type, reference_id, recorded_at, note, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())", Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, userId); statement.setString(2, file.getFileKey()); statement.setString(3, request.getCategory());
            statement.setString(4, request.getTitle()); statement.setString(5, request.getReferenceType()); statement.setString(6, referenceId);
            statement.setObject(7, ReefDateTimeSupport.parseDateTime(request.getRecordedAt())); statement.setString(8, request.getNote());
            return statement;
        }, keyHolder);
        if (keyHolder.getKey() == null) throw new ReefJournalException("备份照片没有保存，请重试");
        Long photoId = keyHolder.getKey().longValue();
        jdbcTemplate.update("INSERT INTO reef_backup_mapping (user_id, backup_key, entity_type, original_id, restored_id) VALUES (?, ?, ?, ?, ?)",
                userId, backupId, BACKUP_PHOTO, request.getOriginalId(), photoId);
        return String.valueOf(photoId);
    }

    /** 将备份内关联编号转换为当前用户已恢复的编号。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    private String restoredReference(Long userId, String backupId, String type, String originalId) {
        if (type == null || type.isEmpty()) return null;
        if (!Arrays.asList(BACKUP_TANK, BACKUP_BIOLOGY, BACKUP_EQUIPMENT, BACKUP_LOG, BACKUP_EVENT).contains(type)
                || originalId == null || originalId.isEmpty()) throw new ReefJournalException("备份照片的关联信息无效");
        List<String> values = jdbcTemplate.query("SELECT CAST(restored_id AS CHAR) FROM reef_backup_mapping WHERE user_id = ? AND backup_key = ? AND entity_type = ? AND original_id = ?",
                (row, index) -> row.getString(1), userId, backupId, type, originalId);
        if (values.isEmpty()) throw new ReefJournalException("备份照片关联的记录尚未恢复");
        return values.get(ZERO);
    }
}
