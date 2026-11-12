package com.tiankong44.tool.reefjournal.service;

import com.tiankong44.tool.reefjournal.dto.ReefBackupBatchRequest;
import com.tiankong44.tool.reefjournal.dto.ReefBackupMappingDto;
import com.tiankong44.tool.reefjournal.dto.ReefBackupRestoreContext;
import com.tiankong44.tool.reefjournal.dto.ReefBackupRestoreResult;
import com.tiankong44.tool.reefjournal.exception.ReefJournalException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import static com.tiankong44.tool.reefjournal.ReefAquariumConstants.BACKUP_BATCH_LIMIT;

/**
 * 将备份分批恢复到当前用户，持久化编号映射并支持重复重试。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
@Service
public class ReefBackupRestoreService {
    private final JdbcTemplate jdbcTemplate;
    private final ReefBackupEntityRestoreService entities;
    private final ReefBackupRecordRestoreService records;
    private final ReefBackupMappingService mappingService;

    /** 创建只编排恢复批次的门面。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    public ReefBackupRestoreService(JdbcTemplate jdbcTemplate, ReefBackupEntityRestoreService entities, ReefBackupRecordRestoreService records,
                                    ReefBackupMappingService mappingService) {
        this.jdbcTemplate = jdbcTemplate; this.entities = entities; this.records = records; this.mappingService = mappingService;
    }

    /**
     * 按依赖顺序恢复一批记录，事务内不读取文件或执行远程调用。
     * @Author zhanghao_SMEICS
     * @Date 2026-11-12
     */
    @Transactional
    public ReefBackupRestoreResult restore(Long userId, ReefBackupBatchRequest request) {
        validateBatch(request);
        String backupId = request.getBackupId();
        try {
            // 同一用户和备份只建立一个批次，不依赖单机状态。
            jdbcTemplate.update("INSERT INTO reef_backup_restore (user_id, backup_key, created_at) VALUES (?, ?, NOW())", userId, backupId);
        } catch (DuplicateKeyException ignored) {
            // 已存在时继续锁定批次并跳过已恢复的记录。
        }
        jdbcTemplate.queryForObject("SELECT id FROM reef_backup_restore WHERE user_id = ? AND backup_key = ? FOR UPDATE", Long.class, userId, backupId);
        // 只读取本批涉及的历史编号，避免备份增大后反复全量扫描。
        ReefBackupRestoreContext context = mappingService.load(userId, backupId, request);
        // 先恢复主档，再恢复引用主档的历史记录。
        entities.tanks(userId, request.getTanks(), context);
        entities.biology(userId, request.getBiology(), context);
        entities.equipment(userId, request.getEquipment(), context);
        records.events(userId, request.getBiologyEvents(), context);
        records.logs(userId, request.getLogs(), context);
        records.ranges(userId, request.getWaterRanges(), context);
        List<Object[]> mappings = new ArrayList<>();
        for (ReefBackupMappingDto item : context.getAdded()) mappings.add(new Object[]{userId, backupId, item.getEntityType(), item.getOriginalId(), Long.valueOf(item.getRestoredId())});
        if (!mappings.isEmpty()) jdbcTemplate.batchUpdate("INSERT INTO reef_backup_mapping (user_id, backup_key, entity_type, original_id, restored_id) VALUES (?, ?, ?, ?, ?)", mappings);
        ReefBackupRestoreResult result = new ReefBackupRestoreResult();
        result.setBackupId(backupId);
        result.setAlreadyRestored(mappings.isEmpty());
        return result;
    }

    /** 校验备份编号和批次大小，限制单次数据库事务的工作量。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    private void validateBatch(ReefBackupBatchRequest request) {
        try { UUID.fromString(request.getBackupId()); }
        catch (RuntimeException exception) { throw new ReefJournalException("备份编号无效"); }
        if (request.getTanks() == null || request.getBiology() == null || request.getEquipment() == null
                || request.getBiologyEvents() == null || request.getLogs() == null || request.getWaterRanges() == null) {
            throw new ReefJournalException("备份批次缺少数据列表");
        }
        int size = request.getTanks().size() + request.getBiology().size() + request.getEquipment().size()
                + request.getBiologyEvents().size() + request.getLogs().size() + request.getWaterRanges().size();
        if (size > BACKUP_BATCH_LIMIT) throw new ReefJournalException("备份批次记录过多，请分批恢复");
        if (request.getTanks().contains(null) || request.getBiology().contains(null) || request.getEquipment().contains(null)
                || request.getBiologyEvents().contains(null) || request.getLogs().contains(null) || request.getWaterRanges().contains(null)) {
            throw new ReefJournalException("备份批次包含空记录，请检查备份文件");
        }
    }
}
