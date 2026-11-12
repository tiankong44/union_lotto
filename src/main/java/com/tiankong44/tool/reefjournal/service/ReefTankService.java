package com.tiankong44.tool.reefjournal.service;

import com.tiankong44.tool.reefjournal.dto.ReefDashboardResponse;
import com.tiankong44.tool.reefjournal.dto.ReefPageResponse;
import com.tiankong44.tool.reefjournal.dto.ReefTankDto;
import com.tiankong44.tool.reefjournal.dto.ReefTankSaveRequest;
import com.tiankong44.tool.reefjournal.dto.ReefWaterRangeDto;
import com.tiankong44.tool.reefjournal.dto.ReefWaterRangeItemRequest;
import com.tiankong44.tool.reefjournal.dto.ReefWaterRangeRequest;
import com.tiankong44.tool.reefjournal.exception.ReefJournalException;
import com.tiankong44.tool.reefjournal.support.ReefDateTimeSupport;
import com.tiankong44.tool.reefjournal.support.ReefPageSupport;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tiankong44.tool.reefjournal.ReefJournalConstants.METRIC_NO2;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.METRIC_NO3;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.METRIC_PH;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.METRIC_PO4;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.METRIC_SALINITY;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.METRIC_TEMPERATURE;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.ONE;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.TIME_ZONE;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.ZERO;
import static com.tiankong44.tool.reefjournal.ReefAquariumConstants.WATER_TYPES;
import static com.tiankong44.tool.reefjournal.ReefAquariumConstants.TANK_THEMES;
import static com.tiankong44.tool.reefjournal.ReefAquariumConstants.WATER_METRIC_UNITS;

/**
 * 维护鱼缸资料、首页摘要及用户设定的水质范围。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Service
public class ReefTankService {
    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建使用项目共享 JDBC 能力的鱼缸服务。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param jdbcTemplate Spring 管理的数据库访问器
     */
    public ReefTankService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 返回当前用户的鱼缸卡片和各缸摘要信息。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 认证上下文中的用户编号
     * @return 鱼缸列表；无鱼缸时返回空数组
     */
    public ReefDashboardResponse dashboard(Long userId) {
        List<ReefTankDto> tanks = jdbcTemplate.query(tankSelect() + " WHERE user_id = ? ORDER BY id DESC",
                BeanPropertyRowMapper.newInstance(ReefTankDto.class), userId);
        Map<Long, Integer> biologyCounts = groupedCounts("SELECT tank_id, SUM(quantity) total FROM reef_biology WHERE user_id = ? GROUP BY tank_id", userId);
        Map<Long, Integer> equipmentCounts = groupedCounts("SELECT tank_id, COUNT(*) total FROM reef_equipment WHERE user_id = ? AND tank_id IS NOT NULL GROUP BY tank_id", userId);
        Map<Long, String> latestRecords = latestRecordSummaries(userId);
        List<ReefTankDto> result = new ArrayList<>();
        for (ReefTankDto tank : tanks) {
            Long tankId = Long.valueOf(tank.getId());
            tank.setDaysOpen(daysSince(tank.getStartedAt()));
            tank.setBiologyCount(biologyCounts.getOrDefault(tankId, ZERO));
            tank.setEquipmentCount(equipmentCounts.getOrDefault(tankId, ZERO));
            tank.setLastRecordSummary(latestRecords.get(tankId));
            result.add(tank);
        }
        return new ReefDashboardResponse(result);
    }

    /**
     * 按用户隔离并分页读取鱼缸资料。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 认证上下文中的用户编号
     * @param requestedPage 客户端页码
     * @param requestedSize 客户端每页条数
     * @return 当前用户的鱼缸分页
     */
    public ReefPageResponse<ReefTankDto> list(Long userId, Integer requestedPage, Integer requestedSize) {
        int pageNum = ReefPageSupport.pageNumber(requestedPage);
        int pageSize = ReefPageSupport.pageSize(requestedSize);
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reef_tank WHERE user_id = ?", Long.class, userId);
        List<ReefTankDto> rows = jdbcTemplate.query(tankSelect() + " WHERE user_id = ? ORDER BY id DESC LIMIT ? OFFSET ?",
                BeanPropertyRowMapper.newInstance(ReefTankDto.class), userId, pageSize, ReefPageSupport.offset(pageNum, pageSize));
        for (ReefTankDto tank : rows) tank.setDaysOpen(daysSince(tank.getStartedAt()));
        return ReefPageSupport.response(pageNum, pageSize, total == null ? ZERO : total, rows);
    }

    /**
     * 读取属于当前用户的单个鱼缸详情。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 认证上下文中的用户编号
     * @param tankId 鱼缸编号
     * @return 鱼缸详情
     */
    public ReefTankDto get(Long userId, Long tankId) {
        List<ReefTankDto> rows = jdbcTemplate.query(tankSelect() + " WHERE user_id = ? AND id = ? LIMIT 1",
                BeanPropertyRowMapper.newInstance(ReefTankDto.class), userId, tankId);
        if (rows.isEmpty()) throw new ReefJournalException("没有找到这个鱼缸，或它不属于当前账号");
        ReefTankDto tank = rows.get(ZERO);
        tank.setDaysOpen(daysSince(tank.getStartedAt()));
        tank.setWaterRanges(readWaterRanges(userId, tankId));
        return tank;
    }

    /**
     * 新增鱼缸或更新当前用户已有鱼缸资料。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 认证上下文中的用户编号
     * @param request 已校验的鱼缸资料
     * @return 保存后的鱼缸详情
     */
    @Transactional
    public ReefTankDto save(Long userId, ReefTankSaveRequest request) {
        // 校验用户选择的水体、主题和水质指标，不推断历史类型。
        validateTankOptions(request);
        LocalDate startedAt = ReefDateTimeSupport.parseDate(request.getStartedAt());
        if (request.getId() == null || request.getId().trim().isEmpty()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            // 写入新鱼缸及用户备注，并读取数据库生成的主键。
            jdbcTemplate.update((Connection connection) -> {
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO reef_tank (user_id, name, started_at, capacity_liter, system_type, note, water_type, themes, enabled_metrics, archived, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                        Statement.RETURN_GENERATED_KEYS);
                statement.setLong(ONE, userId);
                statement.setString(2, request.getName().trim());
                statement.setObject(3, startedAt);
                statement.setBigDecimal(4, request.getCapacityLiter());
                statement.setString(5, request.getSystemType());
                statement.setString(6, request.getNote());
                statement.setString(7, request.getWaterType());
                statement.setString(8, request.getThemes());
                statement.setString(9, request.getEnabledMetrics());
                statement.setBoolean(10, Boolean.TRUE.equals(request.getArchived()));
                return statement;
            }, keyHolder);
            Number id = keyHolder.getKey();
            if (id == null) throw new ReefJournalException("鱼缸资料没有保存，请重试");
            return get(userId, id.longValue());
        }
        Long tankId = parseId(request.getId());
        ensureOwned(userId, tankId);
        // 更新当前用户鱼缸资料并保留页面录入的备注。
        jdbcTemplate.update("UPDATE reef_tank SET name = ?, started_at = ?, capacity_liter = ?, system_type = ?, note = ?, water_type = COALESCE(?, water_type), themes = COALESCE(?, themes), enabled_metrics = COALESCE(?, enabled_metrics), archived = COALESCE(?, archived), updated_at = NOW() WHERE user_id = ? AND id = ?",
                request.getName().trim(), startedAt, request.getCapacityLiter(), request.getSystemType(), request.getNote(),
                request.getWaterType(), request.getThemes(), request.getEnabledMetrics(), request.getArchived(), userId, tankId);
        return get(userId, tankId);
    }

    /**
     * 校验鱼缸可选编码，允许旧资料保留未设置状态。
     * @Author zhanghao_SMEICS
     * @Date 2026-11-12
     * @param request 用户提交的鱼缸资料
     */
    private void validateTankOptions(ReefTankSaveRequest request) {
        if (request.getWaterType() != null && !request.getWaterType().isEmpty() && !WATER_TYPES.contains(request.getWaterType())) {
            throw new ReefJournalException("请选择有效的水体类型");
        }
        if (request.getThemes() != null && !request.getThemes().isEmpty()) {
            for (String theme : request.getThemes().split(",")) {
                if (!TANK_THEMES.contains(theme)) throw new ReefJournalException("饲养主题不受支持");
            }
        }
        if (request.getEnabledMetrics() != null && !request.getEnabledMetrics().isEmpty()) {
            for (String metric : request.getEnabledMetrics().split(",")) {
                if (!WATER_METRIC_UNITS.containsKey(metric)) throw new ReefJournalException("水质指标不受支持");
            }
        }
    }

    /**
     * 覆盖保存用户指定鱼缸的参数范围，未设置上下限的参数不产生自动提醒。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 认证上下文中的用户编号
     * @param tankId 鱼缸编号
     * @param request 用户提交的水质范围列表
     */
    @Transactional
    public void saveWaterRanges(Long userId, Long tankId, ReefWaterRangeRequest request) {
        ensureOwned(userId, tankId);
        for (ReefWaterRangeItemRequest range : request.getRanges()) {
            validateMetric(range);
        }
        jdbcTemplate.update("DELETE FROM reef_water_range WHERE user_id = ? AND tank_id = ?", userId, tankId);
        for (ReefWaterRangeItemRequest range : request.getRanges()) {
            jdbcTemplate.update("INSERT INTO reef_water_range (user_id, tank_id, metric, minimum_value, maximum_value, created_at, updated_at) VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                    userId, tankId, range.getMetric(), range.getMinimum(), range.getMaximum());
        }
    }

    /**
     * 验证参数编码在固定白名单内且上下限关系正确。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param range 单项用户水质范围
     */
    private void validateMetric(ReefWaterRangeItemRequest range) {
        List<String> allowed = Arrays.asList(METRIC_SALINITY, METRIC_TEMPERATURE, METRIC_PH, METRIC_NO2, METRIC_NO3, METRIC_PO4);
        if (range.getMetric() == null || !allowed.contains(range.getMetric())) {
            throw new ReefJournalException("水质参数类型不受支持");
        }
        if (range.getMinimum() != null && range.getMaximum() != null && range.getMinimum().compareTo(range.getMaximum()) > ZERO) {
            throw new ReefJournalException("参数范围下限不能高于上限");
        }
    }

    /**
     * 查询当前用户的鱼缸水质上下限。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 用户编号
     * @param tankId 鱼缸编号
     * @return 水质范围列表
     */
    public List<ReefWaterRangeDto> readWaterRanges(Long userId, Long tankId) {
        List<ReefWaterRangeDto> ranges = jdbcTemplate.query("SELECT metric, minimum_value AS minimum, maximum_value AS maximum FROM reef_water_range WHERE user_id = ? AND tank_id = ? ORDER BY metric",
                BeanPropertyRowMapper.newInstance(ReefWaterRangeDto.class), userId, tankId);
        for (ReefWaterRangeDto range : ranges) decorateRange(range);
        return ranges;
    }

    /**
     * 校验鱼缸归属，防止通过替换路径编号访问其他用户资料。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 认证上下文中的用户编号
     * @param tankId 鱼缸编号
     */
    public void ensureOwned(Long userId, Long tankId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reef_tank WHERE user_id = ? AND id = ?", Integer.class, userId, tankId);
        if (count == null || count != ONE) throw new ReefJournalException("没有找到这个鱼缸，或它不属于当前账号");
    }

    /**
     * 将页面传入的字符串主键解析为正整数。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param rawId 字符串编号
     * @return 数据库主键
     */
    public Long parseId(String rawId) {
        try {
            long value = Long.parseLong(rawId);
            if (value <= ZERO) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException exception) {
            throw new ReefJournalException("记录编号无效");
        }
    }

    /**
     * 返回鱼缸列表查询的固定字段投影。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @return 只包含鱼缸响应字段的查询前缀
     */
    private String tankSelect() {
        return "SELECT CAST(id AS CHAR) id, name, DATE_FORMAT(started_at, '%Y-%m-%d') started_at, CAST(capacity_liter AS CHAR) capacity_liter, system_type, water_type, themes, enabled_metrics, archived, note FROM reef_tank";
    }

    /**
     * 计算开缸日期到当前上海日期的自然日数。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param date 开缸日期文本
     * @return 已开缸天数；日期缺失时返回0
     */
    private int daysSince(String date) {
        return date == null ? ZERO : (int) java.time.temporal.ChronoUnit.DAYS.between(ReefDateTimeSupport.parseDate(date), LocalDate.now(ZoneId.of(TIME_ZONE)));
    }

    /**
     * 将数据库按鱼缸分组的计数结果装配为服务端摘要索引。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param sql 固定维护的分组计数 SQL
     * @param userId 当前用户编号
     * @return 鱼缸编号到记录数的映射
     */
    private Map<Long, Integer> groupedCounts(String sql, Long userId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, userId);
        Map<Long, Integer> counts = new HashMap<>();
        for (Map<String, Object> row : rows) {
            counts.put(((Number) row.get("tank_id")).longValue(), ((Number) row.get("total")).intValue());
        }
        return counts;
    }

    /**
     * 将水质参数编码映射为页面使用的标签和单位。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param range 鱼缸水质范围响应
     */
    private void decorateRange(ReefWaterRangeDto range) {
        if (METRIC_SALINITY.equals(range.getMetric())) {
            range.setLabel("盐度");
            range.setUnit("比重");
        } else if (METRIC_TEMPERATURE.equals(range.getMetric())) {
            range.setLabel("水温");
            range.setUnit("℃");
        } else if (METRIC_PH.equals(range.getMetric())) {
            range.setLabel("pH");
            range.setUnit("");
        } else if (METRIC_NO2.equals(range.getMetric())) {
            range.setLabel("NO₂");
            range.setUnit("mg/L");
        } else if (METRIC_NO3.equals(range.getMetric())) {
            range.setLabel("NO₃");
            range.setUnit("mg/L");
        } else if (METRIC_PO4.equals(range.getMetric())) {
            range.setLabel("PO₄");
            range.setUnit("mg/L");
        }
    }

    /**
     * 一次读取当前用户日志摘要，并在 Java 中保留每个鱼缸的最新一条。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 当前用户编号
     * @return 鱼缸编号到最近日志摘要的映射
     */
    private Map<Long, String> latestRecordSummaries(Long userId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT tank_id, CONCAT(DATE_FORMAT(recorded_at, '%m-%d %H:%i'), ' · ', title) summary FROM reef_log WHERE user_id = ? ORDER BY recorded_at DESC", userId);
        Map<Long, String> summaries = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long tankId = ((Number) row.get("tank_id")).longValue();
            summaries.putIfAbsent(tankId, String.valueOf(row.get("summary")));
        }
        return summaries;
    }
}
