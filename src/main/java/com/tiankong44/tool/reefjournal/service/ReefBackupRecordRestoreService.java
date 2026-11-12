package com.tiankong44.tool.reefjournal.service;

import com.tiankong44.tool.reefjournal.dto.*;
import com.tiankong44.tool.reefjournal.exception.ReefJournalException;
import com.tiankong44.tool.reefjournal.support.ReefDateTimeSupport;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.tiankong44.tool.reefjournal.ReefAquariumConstants.*;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.*;

/**
 * 恢复日志、生物历史事件和鱼缸范围，保留备份中的关联。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
@Service
public class ReefBackupRecordRestoreService {
    private final JdbcTemplate jdbcTemplate;
    private final ReefLogService logs;
    private final ReefBackupValidationService validation;

    /** 创建复用日志保存和归属校验的恢复处理器。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    public ReefBackupRecordRestoreService(JdbcTemplate jdbcTemplate, ReefLogService logs, ReefBackupValidationService validation) {
        this.jdbcTemplate = jdbcTemplate;
        this.logs = logs;
        this.validation = validation;
    }

    /** 恢复历史事件，但不让旧事件改变当前生物状态或当前鱼缸。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    public void events(Long userId, List<ReefBiologyEventDto> rows, ReefBackupRestoreContext context) {
        for (ReefBiologyEventDto row : rows) {
            if (context.contains(BACKUP_EVENT, row.getId())) continue;
            ReefBiologyEventSaveRequest check = new ReefBiologyEventSaveRequest();
            check.setTitle(row.getTitle()); check.setEventAt(row.getEventAt()); check.setDescription(row.getDescription());
            validation.validate(check);
            if (row.getEventType() != null && !BIOLOGY_EVENTS.contains(row.getEventType())) throw new ReefJournalException("备份中的生物事件类型不受支持");
            if (row.getQuantity() != null && row.getQuantity() < ZERO) throw new ReefJournalException("备份中的事件数量无效");
            String biologyId = context.require(BACKUP_BIOLOGY, row.getBiologyId());
            String fromId = row.getFromTankId() == null ? null : context.require(BACKUP_TANK, row.getFromTankId());
            String toId = row.getToTankId() == null ? null : context.require(BACKUP_TANK, row.getToTankId());
            String relatedId = row.getRelatedBiologyId() == null ? null : context.require(BACKUP_BIOLOGY, row.getRelatedBiologyId());
            KeyHolder keyHolder = new GeneratedKeyHolder();
            // 直接保存历史快照，避免重新执行当时的状态变化或转缸动作。
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement("INSERT INTO reef_biology_event (user_id, biology_id, event_at, title, description, status, event_type, from_tank_id, to_tank_id, quantity, related_biology_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())", Statement.RETURN_GENERATED_KEYS);
                statement.setLong(1, userId); statement.setLong(2, Long.parseLong(biologyId));
                statement.setObject(3, ReefDateTimeSupport.parseDateTime(row.getEventAt()));
                statement.setString(4, row.getTitle()); statement.setString(5, row.getDescription());
                statement.setString(6, row.getStatus()); statement.setString(7, row.getEventType());
                statement.setObject(8, fromId == null ? null : Long.valueOf(fromId));
                statement.setObject(9, toId == null ? null : Long.valueOf(toId)); statement.setObject(10, row.getQuantity());
                statement.setObject(11, relatedId == null ? null : Long.valueOf(relatedId));
                return statement;
            }, keyHolder);
            if (keyHolder.getKey() == null) throw new ReefJournalException("备份中的生物事件没有保存");
            context.add(BACKUP_EVENT, row.getId(), String.valueOf(keyHolder.getKey().longValue()));
        }
    }

    /** 恢复日志及其当时关联的生物和水质明细。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    public void logs(Long userId, List<ReefLogDto> rows, ReefBackupRestoreContext context) {
        for (ReefLogDto row : rows) {
            if (context.contains(BACKUP_LOG, row.getId())) continue;
            ReefLogRequest request = logRequest(row, context.require(BACKUP_TANK, row.getTankId()));
            validation.validate(request);
            // 先保存日志主体，历史生物关联随后按备份编号恢复。
            String newId = logs.save(userId, request).getId();
            Set<String> distinct = new HashSet<>();
            List<Object[]> relations = new ArrayList<>();
            if (row.getBiologyIds() != null) for (String oldId : row.getBiologyIds()) {
                String biologyId = context.require(BACKUP_BIOLOGY, oldId);
                if (distinct.add(biologyId)) relations.add(new Object[]{userId, Long.valueOf(newId), Long.valueOf(biologyId)});
            }
            if (!relations.isEmpty()) jdbcTemplate.batchUpdate("INSERT INTO reef_log_biology (user_id, log_id, biology_id) VALUES (?, ?, ?)", relations);
            context.add(BACKUP_LOG, row.getId(), newId);
        }
    }

    /** 转换日志字段并保留未测量与零值的区别。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    private ReefLogRequest logRequest(ReefLogDto row, String tankId) {
        ReefLogRequest request = new ReefLogRequest();
        request.setTankId(tankId); request.setType(row.getType()); request.setTitle(row.getTitle());
        request.setDescription(row.getDescription()); request.setRecordedAt(row.getRecordedAt()); request.setNote(row.getNote());
        request.setChangePercent(validation.decimal(row.getChangePercent())); request.setActivityDetailsProvided(true);
        request.setActivityItem(row.getActivityItem()); request.setActivityAmount(validation.decimal(row.getActivityAmount()));
        request.setActivityUnit(row.getActivityUnit()); request.setWaterSource(row.getWaterSource());
        request.setWaterVolumeLiter(validation.decimal(row.getWaterVolumeLiter())); request.setLightHours(validation.decimal(row.getLightHours()));
        ReefWaterValues water = new ReefWaterValues();
        water.setSalinity(validation.decimal(row.getSalinity())); water.setTemperature(validation.decimal(row.getTemperature()));
        water.setPh(validation.decimal(row.getPh())); water.setNo2(validation.decimal(row.getNo2()));
        water.setNo3(validation.decimal(row.getNo3())); water.setPo4(validation.decimal(row.getPo4()));
        request.setWater(water); request.setMeasurements(row.getMeasurements());
        return request;
    }

    /** 逐缸恢复旧版六项用户自设范围，重复批次只覆盖该备份恢复出的鱼缸。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    public void ranges(Long userId, List<ReefBackupRangeDto> rows, ReefBackupRestoreContext context) {
        for (ReefBackupRangeDto row : rows) {
            if (context.contains(BACKUP_RANGE, row.getId())) continue;
            if (!java.util.Arrays.asList(METRIC_SALINITY, METRIC_TEMPERATURE, METRIC_PH, METRIC_NO2, METRIC_NO3, METRIC_PO4).contains(row.getMetric())) {
                throw new ReefJournalException("备份中的水质范围指标不受支持");
            }
            if (row.getMinimum() != null && row.getMaximum() != null && row.getMinimum().compareTo(row.getMaximum()) > ZERO) {
                throw new ReefJournalException("备份中的水质范围上下限有误");
            }
            Long tankId = Long.valueOf(context.require(BACKUP_TANK, row.getTankId()));
            // 目标缸是本备份新建的当前用户鱼缸，重复恢复时保持幂等。
            jdbcTemplate.update("INSERT INTO reef_water_range (user_id, tank_id, metric, minimum_value, maximum_value, created_at, updated_at) VALUES (?, ?, ?, ?, ?, NOW(), NOW()) ON DUPLICATE KEY UPDATE minimum_value = VALUES(minimum_value), maximum_value = VALUES(maximum_value), updated_at = NOW()",
                    userId, tankId, row.getMetric(), row.getMinimum(), row.getMaximum());
            context.add(BACKUP_RANGE, row.getId(), String.valueOf(tankId));
        }
    }
}
