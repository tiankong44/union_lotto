package com.tiankong44.tool.reefjournal.service;

import com.tiankong44.tool.reefjournal.dto.ReefAlertDto;
import com.tiankong44.tool.reefjournal.dto.ReefLogDto;
import com.tiankong44.tool.reefjournal.dto.ReefLogRequest;
import com.tiankong44.tool.reefjournal.dto.ReefPageResponse;
import com.tiankong44.tool.reefjournal.dto.ReefTrendResponse;
import com.tiankong44.tool.reefjournal.dto.ReefWaterPointDto;
import com.tiankong44.tool.reefjournal.dto.ReefWaterRangeDto;
import com.tiankong44.tool.reefjournal.dto.ReefWaterStatsDto;
import com.tiankong44.tool.reefjournal.dto.ReefWaterValues;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import static com.tiankong44.tool.reefjournal.ReefJournalConstants.METRIC_NO2;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.METRIC_NO3;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.METRIC_PH;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.METRIC_PO4;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.METRIC_SALINITY;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.METRIC_TEMPERATURE;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.ONE;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.PHOTO_REFERENCE_LOG;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.TYPE_WATER_TEST;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.TYPE_WATER_CHANGE;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.TYPE_FEEDING;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.TYPE_MEDICATION;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.TYPE_OBSERVATION;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.ZERO;

/**
 * 保存海缸日志并生成基于用户范围的水质趋势和提醒。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Service
public class ReefLogService {
    private final JdbcTemplate jdbcTemplate;
    private final ReefTankService tankService;

    /**
     * 创建复用鱼缸归属能力的日志服务。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param jdbcTemplate Spring 管理的数据库访问器
     * @param tankService 鱼缸归属服务
     */
    public ReefLogService(JdbcTemplate jdbcTemplate, ReefTankService tankService) {
        this.jdbcTemplate = jdbcTemplate;
        this.tankService = tankService;
    }

    /**
     * 按用户、鱼缸和日志类型分页查询记录。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 认证上下文中的用户编号
     * @param type 可选日志类型
     * @param tankId 可选鱼缸编号
     * @param requestedPage 客户端页码
     * @param requestedSize 客户端每页条数
     * @return 日志分页响应
     */
    public ReefPageResponse<ReefLogDto> list(Long userId, String type, String tankId, Integer requestedPage, Integer requestedSize) {
        int pageNum = ReefPageSupport.pageNumber(requestedPage);
        int pageSize = ReefPageSupport.pageSize(requestedSize);
        List<Object> parameters = new ArrayList<>();
        StringBuilder condition = new StringBuilder(" WHERE user_id = ?");
        parameters.add(userId);
        if (!isBlank(type)) {
            condition.append(" AND type = ?");
            parameters.add(type);
        }
        if (!isBlank(tankId)) {
            condition.append(" AND tank_id = ?");
            parameters.add(tankService.parseId(tankId));
        }
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reef_log" + condition, Long.class, parameters.toArray());
        parameters.add(pageSize);
        parameters.add(ReefPageSupport.offset(pageNum, pageSize));
        List<ReefLogDto> rows = jdbcTemplate.query(logSelect() + condition + " ORDER BY recorded_at DESC, id DESC LIMIT ? OFFSET ?",
                BeanPropertyRowMapper.newInstance(ReefLogDto.class), parameters.toArray());
        return ReefPageSupport.response(pageNum, pageSize, total == null ? ZERO : total, rows);
    }

    /**
     * 保存日志并核验鱼缸、生物引用均属于当前用户。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 认证上下文中的用户编号
     * @param request 已校验日志内容和关联编号
     * @return 保存后的日志详情
     */
    @Transactional
    public ReefLogDto save(Long userId, ReefLogRequest request) {
        validateLogType(request.getType());
        Long tankId = tankService.parseId(request.getTankId());
        tankService.ensureOwned(userId, tankId);
        List<Long> biologyIds = verifyBiologyIds(userId, tankId, request.getBiologyIds());
        LocalDateTime recordedAt = ReefDateTimeSupport.parseDateTime(request.getRecordedAt());
        Long logId = isBlank(request.getId())
                ? insertLog(userId, tankId, request, recordedAt)
                : updateLog(userId, tankId, request, recordedAt);
        jdbcTemplate.update("DELETE FROM reef_log_biology WHERE user_id = ? AND log_id = ?", userId, logId);
        for (Long biologyId : biologyIds) {
            jdbcTemplate.update("INSERT INTO reef_log_biology (user_id, log_id, biology_id) VALUES (?, ?, ?)", userId, logId, biologyId);
        }
        return get(userId, logId);
    }

    /**
     * 读取单条当前用户日志。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 认证上下文中的用户编号
     * @param logId 日志编号
     * @return 日志详情
     */
    public ReefLogDto get(Long userId, Long logId) {
        List<ReefLogDto> rows = jdbcTemplate.query(logSelect() + " WHERE user_id = ? AND id = ? LIMIT 1",
                BeanPropertyRowMapper.newInstance(ReefLogDto.class), userId, logId);
        if (rows.isEmpty()) throw new ReefJournalException("没有找到这条日志，或它不属于当前账号");
        ReefLogDto log = rows.get(ZERO);
        // 读取详情页展示和编辑所需的生物关联编号。
        log.setBiologyIds(jdbcTemplate.query("SELECT CAST(biology_id AS CHAR) FROM reef_log_biology WHERE user_id = ? AND log_id = ? ORDER BY id",
                (resultSet, rowNumber) -> resultSet.getString(1), userId, logId));
        return log;
    }

    /**
     * 删除当前用户的日志，并阻止仍被影像引用的记录被移除。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 认证上下文中的用户编号
     * @param logId 日志编号
     */
    @Transactional
    public void delete(Long userId, Long logId) {
        // 先确认记录归属，避免通过响应差异暴露其他用户数据。
        Integer ownedCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reef_log WHERE user_id = ? AND id = ?", Integer.class, userId, logId);
        if (ownedCount == null || ownedCount != ONE) throw new ReefJournalException("没有找到这条日志，或它不属于当前账号");
        // 保留仍被照片引用的日志，用户可先在照片详情中解除关联。
        Integer photoReferenceCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reef_photo WHERE user_id = ? AND reference_type = ? AND reference_id = ?",
                Integer.class, userId, PHOTO_REFERENCE_LOG, String.valueOf(logId));
        if (photoReferenceCount != null && photoReferenceCount > ZERO) {
            throw new ReefJournalException("这条日志还关联着照片，请先在照片详情中解除关联");
        }
        // 清理日志与生物的关系后，再删除当前用户的日志主体。
        jdbcTemplate.update("DELETE FROM reef_log_biology WHERE user_id = ? AND log_id = ?", userId, logId);
        int deletedCount = jdbcTemplate.update("DELETE FROM reef_log WHERE user_id = ? AND id = ?", userId, logId);
        if (deletedCount != ONE) throw new ReefJournalException("日志没有删除，请刷新后重试");
    }

    /**
     * 查询趋势采样点，并只按用户自行设置的范围生成异常提醒。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 认证上下文中的用户编号
     * @param tankId 鱼缸编号
     * @param metric 用户选中的水质参数
     * @param days 统计天数，零表示全部
     * @return 趋势点、统计摘要和范围提醒
     */
    public ReefTrendResponse trend(Long userId, Long tankId, String metric, Integer days) {
        tankService.ensureOwned(userId, tankId);
        String column = metricColumn(metric);
        int rangeDays = days == null ? 30 : Math.max(days, ZERO);
        LocalDate startDate = rangeDays == ZERO ? null : ReefDateTimeSupport.now().toLocalDate().minusDays(rangeDays);
        List<ReefWaterPointDto> points = trendPoints(userId, tankId, column, startDate);
        ReefTrendResponse response = new ReefTrendResponse();
        response.setPoints(points);
        response.setStats(buildStats(points));
        response.setAlerts(buildAlerts(userId, tankId));
        return response;
    }

    /**
     * 保存新日志主体并返回数据库编号。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 所属用户编号
     * @param tankId 已校验鱼缸编号
     * @param request 日志资料
     * @param recordedAt 上海本地记录时间
     * @return 新日志编号
     */
    private Long insertLog(Long userId, Long tankId, ReefLogRequest request, LocalDateTime recordedAt) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update((Connection connection) -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO reef_log (user_id, tank_id, type, title, description, recorded_at, note, change_percent, salinity, temperature, ph, no2, no3, po4, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(ONE, userId);
            statement.setLong(2, tankId);
            bindLog(statement, request, recordedAt, 3);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) throw new ReefJournalException("日志没有保存，请重试");
        return key.longValue();
    }

    /**
     * 更新当前用户日志主体，确保目标日志已经归属当前账号。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 所属用户编号
     * @param tankId 已校验鱼缸编号
     * @param request 日志资料
     * @param recordedAt 上海本地记录时间
     * @return 已更新日志编号
     */
    private Long updateLog(Long userId, Long tankId, ReefLogRequest request, LocalDateTime recordedAt) {
        Long logId = tankService.parseId(request.getId());
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reef_log WHERE user_id = ? AND id = ?", Integer.class, userId, logId);
        if (count == null || count != ONE) throw new ReefJournalException("没有找到这条日志，或它不属于当前账号");
        jdbcTemplate.update("UPDATE reef_log SET tank_id = ?, type = ?, title = ?, description = ?, recorded_at = ?, note = ?, change_percent = ?, salinity = ?, temperature = ?, ph = ?, no2 = ?, no3 = ?, po4 = ?, updated_at = NOW() WHERE user_id = ? AND id = ?",
                tankId, request.getType(), request.getTitle().trim(), request.getDescription(), recordedAt, request.getNote(),
                request.getChangePercent(), waterValue(request.getWater(), METRIC_SALINITY), waterValue(request.getWater(), METRIC_TEMPERATURE),
                waterValue(request.getWater(), METRIC_PH), waterValue(request.getWater(), METRIC_NO2), waterValue(request.getWater(), METRIC_NO3),
                waterValue(request.getWater(), METRIC_PO4), userId, logId);
        return logId;
    }

    /**
     * 将经过校验的日志 DTO 按固定 SQL 字段顺序绑定。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param statement 预编译 SQL 语句
     * @param request 日志 DTO
     * @param recordedAt 已解析记录时间
     * @param firstIndex 日志类型字段参数位置
     * @throws java.sql.SQLException JDBC 参数绑定失败时抛出
     */
    private void bindLog(PreparedStatement statement, ReefLogRequest request, LocalDateTime recordedAt, int firstIndex) throws java.sql.SQLException {
        statement.setString(firstIndex, request.getType());
        statement.setString(firstIndex + ONE, request.getTitle().trim());
        statement.setString(firstIndex + 2, request.getDescription());
        statement.setObject(firstIndex + 3, recordedAt);
        statement.setString(firstIndex + 4, request.getNote());
        statement.setBigDecimal(firstIndex + 5, request.getChangePercent());
        statement.setBigDecimal(firstIndex + 6, waterValue(request.getWater(), METRIC_SALINITY));
        statement.setBigDecimal(firstIndex + 7, waterValue(request.getWater(), METRIC_TEMPERATURE));
        statement.setBigDecimal(firstIndex + 8, waterValue(request.getWater(), METRIC_PH));
        statement.setBigDecimal(firstIndex + 9, waterValue(request.getWater(), METRIC_NO2));
        statement.setBigDecimal(firstIndex + 10, waterValue(request.getWater(), METRIC_NO3));
        statement.setBigDecimal(firstIndex + 11, waterValue(request.getWater(), METRIC_PO4));
    }

    /**
     * 批量检查关联生物都属于当前用户和目标鱼缸。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 当前用户编号
     * @param tankId 目标鱼缸编号
     * @param rawIds 客户端生物编号集合
     * @return 去重后的有效生物编号
     */
    private List<Long> verifyBiologyIds(Long userId, Long tankId, List<String> rawIds) {
        List<Long> ids = new ArrayList<>();
        if (rawIds == null || rawIds.isEmpty()) return ids;
        for (String rawId : rawIds) ids.add(tankService.parseId(rawId));
        if (ids.size() > 50) throw new ReefJournalException("单条日志最多关联50个生物");
        StringJoiner placeholders = new StringJoiner(",");
        for (int index = ZERO; index < ids.size(); index++) placeholders.add("?");
        List<Object> parameters = new ArrayList<>();
        parameters.add(userId);
        parameters.add(tankId);
        parameters.addAll(ids);
        Integer owned = jdbcTemplate.queryForObject("SELECT COUNT(DISTINCT id) FROM reef_biology WHERE user_id = ? AND tank_id = ? AND id IN (" + placeholders + ")",
                Integer.class, parameters.toArray());
        if (owned == null || owned != ids.stream().distinct().count()) {
            throw new ReefJournalException("关联的生物记录不属于当前鱼缸或账号");
        }
        return ids.stream().distinct().collect(java.util.stream.Collectors.toList());
    }

    /**
     * 按白名单列名和日期范围读取图表采样点。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 当前用户编号
     * @param tankId 鱼缸编号
     * @param column 已由固定白名单解析的数据库列名
     * @param startDate 可选开始日期
     * @return 升序水质采样点
     */
    private List<ReefWaterPointDto> trendPoints(Long userId, Long tankId, String column, LocalDate startDate) {
        String sql = "SELECT DATE_FORMAT(recorded_at, '%Y-%m-%d %H:%i:%s') recorded_at, CAST(" + column + " AS CHAR) value FROM reef_log WHERE user_id = ? AND tank_id = ? AND type = ? AND " + column + " IS NOT NULL";
        List<Object> parameters = new ArrayList<>();
        parameters.add(userId);
        parameters.add(tankId);
        parameters.add(TYPE_WATER_TEST);
        if (startDate != null) {
            sql += " AND recorded_at >= ?";
            parameters.add(startDate.atStartOfDay());
        }
        sql += " ORDER BY recorded_at ASC";
        return jdbcTemplate.query(sql, BeanPropertyRowMapper.newInstance(ReefWaterPointDto.class), parameters.toArray());
    }

    /**
     * 根据用户记录计算当前值、七日均值和相邻采样趋势方向。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param points 当前筛选范围内的采样点
     * @return 趋势统计摘要
     */
    private ReefWaterStatsDto buildStats(List<ReefWaterPointDto> points) {
        ReefWaterStatsDto stats = new ReefWaterStatsDto();
        if (points.isEmpty()) return stats;
        stats.setCurrentValue(points.get(points.size() - ONE).getValue());
        List<ReefWaterPointDto> lastSevenDays = new ArrayList<>();
        LocalDateTime threshold = ReefDateTimeSupport.now().minusDays(7);
        for (ReefWaterPointDto point : points) {
            if (!ReefDateTimeSupport.parseDateTime(point.getRecordedAt()).isBefore(threshold)) lastSevenDays.add(point);
        }
        if (!lastSevenDays.isEmpty()) {
            double average = lastSevenDays.stream().mapToDouble(item -> Double.parseDouble(item.getValue())).average().orElse(ZERO);
            stats.setAverage7Days(String.valueOf(average));
        }
        stats.setTrendKey("STABLE");
        stats.setTrendLabel("稳定");
        if (points.size() > ONE) {
            int comparison = new java.math.BigDecimal(points.get(points.size() - ONE).getValue())
                    .compareTo(new java.math.BigDecimal(points.get(points.size() - 2).getValue()));
            if (comparison > ZERO) {
                stats.setTrendKey("UP");
                stats.setTrendLabel("上升");
            } else if (comparison < ZERO) {
                stats.setTrendKey("DOWN");
                stats.setTrendLabel("下降");
            }
        }
        return stats;
    }

    /**
     * 只比较最新检测值和鱼缸的用户自设参数范围。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 当前用户编号
     * @param tankId 鱼缸编号
     * @return 超出用户范围的提醒；未超出时为空数组
     */
    private List<ReefAlertDto> buildAlerts(Long userId, Long tankId) {
        List<ReefWaterRangeDto> ranges = tankService.readWaterRanges(userId, tankId);
        List<ReefLogDto> latestRows = jdbcTemplate.query(logSelect() + " WHERE user_id = ? AND tank_id = ? AND type = ? ORDER BY recorded_at DESC, id DESC LIMIT 1",
                BeanPropertyRowMapper.newInstance(ReefLogDto.class), userId, tankId, TYPE_WATER_TEST);
        List<ReefAlertDto> alerts = new ArrayList<>();
        if (latestRows.isEmpty()) return alerts;
        ReefLogDto latest = latestRows.get(ZERO);
        for (ReefWaterRangeDto range : ranges) {
            String value = valueForMetric(latest, range.getMetric());
            if (value == null) continue;
            java.math.BigDecimal number = new java.math.BigDecimal(value);
            if (range.getMinimum() != null && number.compareTo(range.getMinimum()) < ZERO) {
                alerts.add(alert(range.getMetric(), "LOW", value + " 低于你设定的范围"));
            }
            if (range.getMaximum() != null && number.compareTo(range.getMaximum()) > ZERO) {
                alerts.add(alert(range.getMetric(), "HIGH", value + " 高于你设定的范围"));
            }
        }
        return alerts;
    }

    /**
     * 组装单条用户自设范围提醒。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param metric 水质参数编码
     * @param level 高于或低于范围
     * @param message 展示文案
     * @return 异常提醒 DTO
     */
    private ReefAlertDto alert(String metric, String level, String message) {
        ReefAlertDto alert = new ReefAlertDto();
        alert.setId(metric + level);
        alert.setLevel(level);
        alert.setMessage(message);
        return alert;
    }

    /**
     * 从最新检测日志中读取指定参数值。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param log 最新水质检测日志
     * @param metric 水质参数编码
     * @return 对应测量值文本
     */
    private String valueForMetric(ReefLogDto log, String metric) {
        if (METRIC_SALINITY.equals(metric)) return log.getSalinity();
        if (METRIC_TEMPERATURE.equals(metric)) return log.getTemperature();
        if (METRIC_PH.equals(metric)) return log.getPh();
        if (METRIC_NO2.equals(metric)) return log.getNo2();
        if (METRIC_NO3.equals(metric)) return log.getNo3();
        if (METRIC_PO4.equals(metric)) return log.getPo4();
        return null;
    }

    /**
     * 从日志输入 DTO 中选择指定水质值。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param water 水质数据 DTO
     * @param metric 水质参数编码
     * @return 对应数值；水质组或字段未填时返回空值
     */
    private java.math.BigDecimal waterValue(ReefWaterValues water, String metric) {
        if (water == null) return null;
        if (METRIC_SALINITY.equals(metric)) return water.getSalinity();
        if (METRIC_TEMPERATURE.equals(metric)) return water.getTemperature();
        if (METRIC_PH.equals(metric)) return water.getPh();
        if (METRIC_NO2.equals(metric)) return water.getNo2();
        if (METRIC_NO3.equals(metric)) return water.getNo3();
        return water.getPo4();
    }

    /**
     * 将趋势指标编码映射到固定 SQL 列名白名单。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param metric 客户端指标编码
     * @return 白名单中的数据库列名
     */
    private String metricColumn(String metric) {
        if (METRIC_SALINITY.equals(metric)) return "salinity";
        if (METRIC_PH.equals(metric)) return "ph";
        if (METRIC_NO3.equals(metric)) return "no3";
        if (METRIC_NO2.equals(metric)) return "no2";
        if (METRIC_PO4.equals(metric)) return "po4";
        throw new ReefJournalException("该水质参数暂不支持趋势查询");
    }

    /**
     * 返回日志响应使用的固定字段投影。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @return 参数化查询前缀
     */
    private String logSelect() {
        return "SELECT CAST(id AS CHAR) id, CAST(tank_id AS CHAR) tank_id, type, title, description, DATE_FORMAT(recorded_at, '%Y-%m-%d %H:%i:%s') recorded_at, note, CAST(change_percent AS CHAR) change_percent, CAST(salinity AS CHAR) salinity, CAST(temperature AS CHAR) temperature, CAST(ph AS CHAR) ph, CAST(no2 AS CHAR) no2, CAST(no3 AS CHAR) no3, CAST(po4 AS CHAR) po4 FROM reef_log";
    }

    /**
     * 判断可选字符串是否为空白。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param value 待判断文本
     * @return 空文本或空白文本时返回 true
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 校验日志类型只能来自小程序提供的选项。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param type 客户端日志类型编码
     */
    private void validateLogType(String type) {
        if (!TYPE_WATER_TEST.equals(type) && !TYPE_WATER_CHANGE.equals(type)
                && !TYPE_FEEDING.equals(type) && !TYPE_MEDICATION.equals(type)
                && !TYPE_OBSERVATION.equals(type)) {
            throw new ReefJournalException("日志类型不受支持");
        }
    }
}
