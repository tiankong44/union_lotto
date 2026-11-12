package com.tiankong44.tool.reefjournal.service;

import com.tiankong44.tool.reefjournal.dto.*;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import static com.tiankong44.tool.reefjournal.ReefAquariumConstants.*;

/**
 * 按当前恢复批次涉及的编号分批读取映射，避免反复扫描整个备份。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
@Service
public class ReefBackupMappingService {
    private final JdbcTemplate jdbcTemplate;

    /** 创建映射查询服务。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    public ReefBackupMappingService(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    /** 收集本批主键与引用，再按类型读取已恢复的编号。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    public ReefBackupRestoreContext load(Long userId, String backupId, ReefBackupBatchRequest request) {
        Map<String, Set<String>> needed = new HashMap<>();
        for (ReefTankDto row : request.getTanks()) add(needed, BACKUP_TANK, row.getId());
        for (ReefBiologyDto row : request.getBiology()) {
            add(needed, BACKUP_BIOLOGY, row.getId()); add(needed, BACKUP_TANK, row.getTankId());
        }
        for (ReefEquipmentDto row : request.getEquipment()) {
            add(needed, BACKUP_EQUIPMENT, row.getId()); add(needed, BACKUP_TANK, row.getTankId());
        }
        for (ReefBiologyEventDto row : request.getBiologyEvents()) {
            add(needed, BACKUP_EVENT, row.getId()); add(needed, BACKUP_BIOLOGY, row.getBiologyId());
            add(needed, BACKUP_BIOLOGY, row.getRelatedBiologyId());
            add(needed, BACKUP_TANK, row.getFromTankId()); add(needed, BACKUP_TANK, row.getToTankId());
        }
        for (ReefLogDto row : request.getLogs()) {
            add(needed, BACKUP_LOG, row.getId()); add(needed, BACKUP_TANK, row.getTankId());
            if (row.getBiologyIds() != null) for (String id : row.getBiologyIds()) add(needed, BACKUP_BIOLOGY, id);
        }
        for (ReefBackupRangeDto row : request.getWaterRanges()) {
            add(needed, BACKUP_RANGE, row.getId()); add(needed, BACKUP_TANK, row.getTankId());
        }
        List<ReefBackupMappingDto> rows = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : needed.entrySet()) {
            List<String> ids = new ArrayList<>(entry.getValue());
            for (int offset = 0; offset < ids.size(); offset += BACKUP_BATCH_LIMIT) {
                rows.addAll(find(userId, backupId, entry.getKey(), ids.subList(offset, Math.min(offset + BACKUP_BATCH_LIMIT, ids.size()))));
            }
        }
        return new ReefBackupRestoreContext(rows);
    }

    /** 收集非空的备份编号。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    private void add(Map<String, Set<String>> needed, String type, String id) {
        if (id != null && !id.isEmpty()) needed.computeIfAbsent(type, key -> new LinkedHashSet<>()).add(id);
    }

    /** 使用参数化条件读取当前用户的一组编号映射。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    private List<ReefBackupMappingDto> find(Long userId, String backupId, String type, List<String> ids) {
        StringJoiner placeholders = new StringJoiner(",");
        List<Object> parameters = new ArrayList<>();
        parameters.add(userId); parameters.add(backupId); parameters.add(type);
        for (String id : ids) { placeholders.add("?"); parameters.add(id); }
        return jdbcTemplate.query("SELECT entity_type, original_id, CAST(restored_id AS CHAR) restored_id FROM reef_backup_mapping WHERE user_id = ? AND backup_key = ? AND entity_type = ? AND original_id IN (" + placeholders + ")",
                BeanPropertyRowMapper.newInstance(ReefBackupMappingDto.class), parameters.toArray());
    }
}
