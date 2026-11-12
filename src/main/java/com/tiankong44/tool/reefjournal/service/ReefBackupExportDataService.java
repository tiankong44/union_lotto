package com.tiankong44.tool.reefjournal.service;

import com.tiankong44.tool.reefjournal.dto.ReefBackupLogBiologyDto;
import com.tiankong44.tool.reefjournal.dto.ReefBackupMeasurementDto;
import com.tiankong44.tool.reefjournal.dto.ReefBackupRangeDto;
import com.tiankong44.tool.reefjournal.dto.ReefLogDto;
import com.tiankong44.tool.reefjournal.dto.ReefWaterMeasurementDto;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 批量补齐备份中的日志关联、水质明细和鱼缸范围。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
@Service
public class ReefBackupExportDataService {
    private final JdbcTemplate jdbcTemplate;

    /** 创建备份数据读取服务。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    public ReefBackupExportDataService(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    /** 按用户读取全部鱼缸范围，不将不同鱼缸的范围混在一起。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    public List<ReefBackupRangeDto> ranges(Long userId) {
        return jdbcTemplate.query("SELECT CAST(id AS CHAR) id, CAST(tank_id AS CHAR) tank_id, metric, minimum_value minimum, maximum_value maximum FROM reef_water_range WHERE user_id = ? ORDER BY tank_id, id",
                BeanPropertyRowMapper.newInstance(ReefBackupRangeDto.class), userId);
    }

    /** 一次补齐一页日志的生物关联和测量明细，避免逐条查询。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    public void populateLogs(Long userId, List<ReefLogDto> logs) {
        if (logs.isEmpty()) return;
        StringJoiner placeholders = new StringJoiner(",");
        List<Object> parameters = new ArrayList<>();
        Map<String, ReefLogDto> byId = new HashMap<>();
        parameters.add(userId);
        for (ReefLogDto log : logs) {
            placeholders.add("?");
            parameters.add(Long.valueOf(log.getId()));
            byId.put(log.getId(), log);
        }
        // 日志与生物关系保持字符串编号，避免前端精度丢失。
        List<ReefBackupLogBiologyDto> relations = jdbcTemplate.query("SELECT CAST(log_id AS CHAR) log_id, CAST(biology_id AS CHAR) biology_id FROM reef_log_biology WHERE user_id = ? AND log_id IN (" + placeholders + ") ORDER BY id",
                BeanPropertyRowMapper.newInstance(ReefBackupLogBiologyDto.class), parameters.toArray());
        for (ReefBackupLogBiologyDto relation : relations) byId.get(relation.getLogId()).getBiologyIds().add(relation.getBiologyId());
        // 一次读取当前页所有测量，不逐条扫描日志。
        List<ReefBackupMeasurementDto> measurements = jdbcTemplate.query("SELECT CAST(log_id AS CHAR) log_id, metric, CAST(measured_value AS CHAR) value, unit, method FROM reef_water_measurement WHERE user_id = ? AND log_id IN (" + placeholders + ") ORDER BY id",
                BeanPropertyRowMapper.newInstance(ReefBackupMeasurementDto.class), parameters.toArray());
        for (ReefBackupMeasurementDto measurement : measurements) {
            ReefWaterMeasurementDto item = new ReefWaterMeasurementDto();
            item.setMetric(measurement.getMetric()); item.setValue(measurement.getValue());
            item.setUnit(measurement.getUnit()); item.setMethod(measurement.getMethod());
            byId.get(measurement.getLogId()).getMeasurements().add(item);
        }
    }
}
