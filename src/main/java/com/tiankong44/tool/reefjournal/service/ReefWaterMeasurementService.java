package com.tiankong44.tool.reefjournal.service;

import com.tiankong44.tool.reefjournal.dto.ReefWaterHistoryDto;
import com.tiankong44.tool.reefjournal.dto.ReefWaterMeasurementDto;
import com.tiankong44.tool.reefjournal.exception.ReefJournalException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.tiankong44.tool.reefjournal.ReefAquariumConstants.WATER_METRIC_UNITS;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.METRIC_TEMPERATURE;

/**
 * 保存逐项水质测量及查询历史，旧日志的六项数值仍保留原字段。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
@Service
public class ReefWaterMeasurementService {
    private final JdbcTemplate jdbcTemplate;

    /** 创建复用项目 JDBC 的水质明细服务。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    public ReefWaterMeasurementService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 校验并替换日志测量明细；未提交明细的旧客户端只更新所属鱼缸和时间。
     * @Author zhanghao_SMEICS
     * @Date 2026-11-12
     */
    public void save(Long userId, Long tankId, Long logId, LocalDateTime recordedAt, List<ReefWaterMeasurementDto> measurements) {
        if (measurements == null) {
            // 旧客户端没有明细字段时保留其已有数据，避免编辑日志造成丢失。
            jdbcTemplate.update("UPDATE reef_water_measurement SET tank_id = ?, recorded_at = ? WHERE user_id = ? AND log_id = ?", tankId, recordedAt, userId, logId);
            return;
        }
        Set<String> seen = new HashSet<>();
        List<Object[]> rows = new ArrayList<>();
        for (ReefWaterMeasurementDto item : measurements) {
            // 逐项校验编码、单位和数值，不将缺失值当作零。
            if (item == null || !WATER_METRIC_UNITS.containsKey(item.getMetric()) || !seen.add(item.getMetric())) {
                throw new ReefJournalException("水质指标无效或重复，请检查后重试");
            }
            String unit = WATER_METRIC_UNITS.get(item.getMetric());
            if (item.getUnit() != null && !unit.equals(item.getUnit())) throw new ReefJournalException("水质指标与单位不匹配");
            BigDecimal value;
            try {
                value = new BigDecimal(item.getValue());
            } catch (RuntimeException exception) {
                throw new ReefJournalException("水质测量值不是有效数字");
            }
            if (value.precision() > 16 || Math.max(value.scale(), 0) > 6 || value.abs().compareTo(new BigDecimal("10000000000")) >= 0) {
                throw new ReefJournalException("水质测量值超出可记录的精度或范围");
            }
            if (!METRIC_TEMPERATURE.equals(item.getMetric()) && value.signum() < 0) throw new ReefJournalException("该水质测量值不能小于零");
            rows.add(new Object[]{userId, tankId, logId, recordedAt, item.getMetric(), value, unit, item.getMethod()});
        }
        // 与日志主体处于同一事务，避免部分测量值被覆盖。
        jdbcTemplate.update("DELETE FROM reef_water_measurement WHERE user_id = ? AND log_id = ?", userId, logId);
        if (!rows.isEmpty()) jdbcTemplate.batchUpdate("INSERT INTO reef_water_measurement (user_id, tank_id, log_id, recorded_at, metric, measured_value, unit, method) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", rows);
    }

    /** 读取单条日志的测量明细。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    public List<ReefWaterMeasurementDto> forLog(Long userId, Long logId) {
        return jdbcTemplate.query("SELECT metric, CAST(measured_value AS CHAR) value, unit, method FROM reef_water_measurement WHERE user_id = ? AND log_id = ? ORDER BY id",
                BeanPropertyRowMapper.newInstance(ReefWaterMeasurementDto.class), userId, logId);
    }

    /** 读取一个鱼缸指定范围内的测量历史。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    public List<ReefWaterHistoryDto> history(Long userId, Long tankId, LocalDate startDate) {
        String sql = "SELECT metric, DATE_FORMAT(recorded_at, '%Y-%m-%d %H:%i:%s') recorded_at, CAST(measured_value AS CHAR) value FROM reef_water_measurement WHERE user_id = ? AND tank_id = ?";
        if (startDate == null) {
            return jdbcTemplate.query(sql + " ORDER BY recorded_at, id", BeanPropertyRowMapper.newInstance(ReefWaterHistoryDto.class), userId, tankId);
        }
        return jdbcTemplate.query(sql + " AND recorded_at >= ? ORDER BY recorded_at, id", BeanPropertyRowMapper.newInstance(ReefWaterHistoryDto.class), userId, tankId, startDate.atStartOfDay());
    }

    /** 删除日志时同步删除所属测量明细。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    public void deleteForLog(Long userId, Long logId) {
        jdbcTemplate.update("DELETE FROM reef_water_measurement WHERE user_id = ? AND log_id = ?", userId, logId);
    }
}
