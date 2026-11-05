package com.tiankong44.tool.reefjournal.service;

import com.tiankong44.tool.reefjournal.dto.ReefEquipmentDto;
import com.tiankong44.tool.reefjournal.dto.ReefEquipmentSaveRequest;
import com.tiankong44.tool.reefjournal.dto.ReefPageResponse;
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
import java.util.List;
import java.util.Arrays;

import static com.tiankong44.tool.reefjournal.ReefJournalConstants.ONE;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.ZERO;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.EQUIPMENT_STATUS_RUNNING;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.EQUIPMENT_STATUS_DIRTY;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.EQUIPMENT_STATUS_STANDBY;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.TIME_ZONE;

/**
 * 管理用户设备资料及其维护周期。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Service
public class ReefEquipmentService {
    private final JdbcTemplate jdbcTemplate;
    private final ReefTankService tankService;

    /**
     * 创建复用鱼缸归属校验的设备服务。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param jdbcTemplate Spring 管理的数据库访问器
     * @param tankService 鱼缸归属服务
     */
    public ReefEquipmentService(JdbcTemplate jdbcTemplate, ReefTankService tankService) {
        this.jdbcTemplate = jdbcTemplate;
        this.tankService = tankService;
    }

    /**
     * 按用户和设备分类分页读取设备。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 认证上下文中的用户编号
     * @param category 可选设备分类
     * @param tankId 可选鱼缸编号
     * @param requestedPage 客户端页码
     * @param requestedSize 客户端每页条数
     * @return 设备分页
     */
    public ReefPageResponse<ReefEquipmentDto> list(Long userId, String category, String tankId, Integer requestedPage, Integer requestedSize) {
        validateCategory(category);
        Long selectedTankId = tankId == null || tankId.trim().isEmpty() ? null : tankService.parseId(tankId);
        if (selectedTankId != null) tankService.ensureOwned(userId, selectedTankId);
        int pageNum = ReefPageSupport.pageNumber(requestedPage);
        int pageSize = ReefPageSupport.pageSize(requestedSize);
        StringBuilder filter = new StringBuilder(" WHERE user_id = ?");
        List<Object> parameters = new ArrayList<>();
        parameters.add(userId);
        if (selectedTankId != null) {
            filter.append(" AND tank_id = ?");
            parameters.add(selectedTankId);
        }
        if (category != null && !category.trim().isEmpty()) {
            filter.append(" AND category = ?");
            parameters.add(category);
        }
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reef_equipment" + filter, Long.class, parameters.toArray());
        parameters.add(pageSize);
        parameters.add(ReefPageSupport.offset(pageNum, pageSize));
        List<ReefEquipmentDto> rows = jdbcTemplate.query(equipmentSelect() + filter + " ORDER BY id DESC LIMIT ? OFFSET ?",
                BeanPropertyRowMapper.newInstance(ReefEquipmentDto.class), parameters.toArray());
        decorate(rows);
        return ReefPageSupport.response(pageNum, pageSize, total == null ? ZERO : total, rows);
    }

    /**
     * 新增设备或更新当前用户设备资料。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 认证上下文中的用户编号
     * @param request 已校验设备资料
     * @return 保存后的设备详情
     */
    @Transactional
    public ReefEquipmentDto save(Long userId, ReefEquipmentSaveRequest request) {
        validateCategory(request.getCategory());
        Long tankId = null;
        if (request.getTankId() != null && !request.getTankId().trim().isEmpty()) {
            tankId = tankService.parseId(request.getTankId());
            tankService.ensureOwned(userId, tankId);
        }
        LocalDate startedAt = parseOptionalDate(request.getStartedAt());
        LocalDate lastMaintainedAt = parseOptionalDate(request.getLastMaintainedAt());
        String status = normalizeStatus(request.getStatus());
        if (request.getMaintenanceIntervalDays() != null && request.getMaintenanceIntervalDays() <= ZERO) {
            throw new ReefJournalException("维护周期需要大于0天");
        }
        if (request.getId() == null || request.getId().trim().isEmpty()) {
            Long id = insert(userId, tankId, request, status, startedAt, lastMaintainedAt);
            return get(userId, id);
        }
        Long equipmentId = tankService.parseId(request.getId());
        ensureOwned(userId, equipmentId);
        // 保存设备功率及维护资料，更新范围限定为当前用户。
        jdbcTemplate.update("UPDATE reef_equipment SET tank_id = ?, category = ?, name = ?, model = ?, status = ?, power_watt = ?, daily_hours = ?, started_at = ?, maintenance_interval_days = ?, last_maintained_at = ?, updated_at = NOW() WHERE user_id = ? AND id = ?",
                tankId, request.getCategory(), request.getName().trim(), request.getModel(), status, request.getPowerWatt(), request.getDailyHours(), startedAt,
                request.getMaintenanceIntervalDays(), lastMaintainedAt, userId, equipmentId);
        return get(userId, equipmentId);
    }

    /**
     * 读取属于当前用户的设备。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 认证上下文中的用户编号
     * @param equipmentId 设备编号
     * @return 设备详情
     */
    public ReefEquipmentDto get(Long userId, Long equipmentId) {
        List<ReefEquipmentDto> rows = jdbcTemplate.query(equipmentSelect() + " WHERE user_id = ? AND id = ? LIMIT 1",
                BeanPropertyRowMapper.newInstance(ReefEquipmentDto.class), userId, equipmentId);
        if (rows.isEmpty()) throw new ReefJournalException("没有找到这台设备，或它不属于当前账号");
        decorate(rows);
        return rows.get(ZERO);
    }

    /**
     * 校验设备分类，防止接收未定义的状态值。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param category 设备分类编码
     */
    private void validateCategory(String category) {
        if (category == null || category.trim().isEmpty()) return;
        if (!Arrays.asList("LIGHT", "FILTER", "TEMPERATURE", "OTHER").contains(category)) {
            throw new ReefJournalException("设备分类不受支持");
        }
    }

    /**
     * 写入新设备并返回数据库生成的主键。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 所属用户编号
     * @param tankId 可选鱼缸编号
     * @param request 设备资料请求
     * @param status 规范化设备状态
     * @param startedAt 运行开始日期
     * @param lastMaintainedAt 上次维护日期
     * @return 新设备编号
     */
    private Long insert(Long userId, Long tankId, ReefEquipmentSaveRequest request, String status, LocalDate startedAt, LocalDate lastMaintainedAt) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        // 保存设备功率和维护资料，并读取数据库生成的主键。
        jdbcTemplate.update((Connection connection) -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO reef_equipment (user_id, tank_id, category, name, model, status, power_watt, daily_hours, started_at, maintenance_interval_days, last_maintained_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(ONE, userId);
            statement.setObject(2, tankId);
            statement.setString(3, request.getCategory());
            statement.setString(4, request.getName().trim());
            statement.setString(5, request.getModel());
            statement.setString(6, status);
            statement.setBigDecimal(7, request.getPowerWatt());
            statement.setBigDecimal(8, request.getDailyHours());
            statement.setObject(9, startedAt);
            statement.setObject(10, request.getMaintenanceIntervalDays());
            statement.setObject(11, lastMaintainedAt);
            return statement;
        }, keyHolder);
        Number id = keyHolder.getKey();
        if (id == null) throw new ReefJournalException("设备资料没有保存，请重试");
        return id.longValue();
    }

    /**
     * 校验设备主键属于当前登录用户。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 当前用户编号
     * @param equipmentId 设备编号
     */
    private void ensureOwned(Long userId, Long equipmentId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reef_equipment WHERE user_id = ? AND id = ?", Integer.class, userId, equipmentId);
        if (count == null || count != ONE) throw new ReefJournalException("没有找到这台设备，或它不属于当前账号");
    }

    /**
     * 批量计算运行天数并生成设备状态标签。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param rows 当前页设备记录
     */
    private void decorate(List<ReefEquipmentDto> rows) {
        for (ReefEquipmentDto row : rows) {
            row.setDaysRunning(row.getStartedAt() == null ? null : (int) java.time.temporal.ChronoUnit.DAYS.between(
                    ReefDateTimeSupport.parseDate(row.getStartedAt()), LocalDate.now(ZoneId.of(TIME_ZONE))));
            row.setStatusLabel(statusLabel(row.getStatus()));
        }
    }

    /**
     * 解析可选设备日期，空文本按未填写处理。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param value 日期文本
     * @return 本地日期或空值
     */
    private LocalDate parseOptionalDate(String value) {
        return value == null || value.trim().isEmpty() ? null : ReefDateTimeSupport.parseDate(value);
    }

    /**
     * 规范化并校验设备状态编码。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param status 用户提交的设备状态
     * @return 可保存的设备状态编码
     */
    private String normalizeStatus(String status) {
        if (status == null || status.trim().isEmpty()) return EQUIPMENT_STATUS_RUNNING;
        if (Arrays.asList(EQUIPMENT_STATUS_RUNNING, EQUIPMENT_STATUS_DIRTY, EQUIPMENT_STATUS_STANDBY).contains(status)) return status;
        throw new ReefJournalException("设备状态不受支持");
    }

    /**
     * 将设备运行状态编码转换为页面标签。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param status 设备状态编码
     * @return 中文设备状态
     */
    private String statusLabel(String status) {
        if (EQUIPMENT_STATUS_RUNNING.equals(status)) return "运行中";
        if (EQUIPMENT_STATUS_DIRTY.equals(status)) return "待清洗";
        if (EQUIPMENT_STATUS_STANDBY.equals(status)) return "待机";
        return "待补充";
    }

    /**
     * 返回设备列表响应所需的固定字段投影。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @return 参数化查询前缀
     */
    private String equipmentSelect() {
        return "SELECT CAST(id AS CHAR) id, CAST(tank_id AS CHAR) tank_id, category, name, model, status, CAST(power_watt AS CHAR) power_watt, CAST(daily_hours AS CHAR) daily_hours, DATE_FORMAT(started_at, '%Y-%m-%d') started_at, maintenance_interval_days, DATE_FORMAT(last_maintained_at, '%Y-%m-%d') last_maintained_at FROM reef_equipment";
    }
}
