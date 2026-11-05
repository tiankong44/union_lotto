package com.tiankong44.tool.reefjournal.service;

import com.tiankong44.tool.reefjournal.dto.ReefBiologyDto;
import com.tiankong44.tool.reefjournal.dto.ReefBiologyEventDto;
import com.tiankong44.tool.reefjournal.dto.ReefBiologyEventSaveRequest;
import com.tiankong44.tool.reefjournal.dto.ReefBiologySaveRequest;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static com.tiankong44.tool.reefjournal.ReefJournalConstants.ONE;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.BIOLOGY_STATUS_NORMAL;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.BIOLOGY_STATUS_QUARANTINE;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.BIOLOGY_STATUS_ISOLATED;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.BIOLOGY_STATUS_SICK;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.BIOLOGY_STATUS_DEAD;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.CATEGORY_FISH;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.CATEGORY_CORAL;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.CATEGORY_PLANT;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.CATEGORY_OTHER;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.TIME_ZONE;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.ZERO;

/**
 * 维护鱼缸生物资料及其事件时间轴。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Service
public class ReefBiologyService {
    private final JdbcTemplate jdbcTemplate;
    private final ReefTankService tankService;

    /**
     * 创建复用鱼缸归属校验的生物服务。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param jdbcTemplate Spring 管理的数据库访问器
     * @param tankService 鱼缸归属服务
     */
    public ReefBiologyService(JdbcTemplate jdbcTemplate, ReefTankService tankService) {
        this.jdbcTemplate = jdbcTemplate;
        this.tankService = tankService;
    }

    /**
     * 按鱼缸分页读取当前用户的生物资料。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 认证上下文中的用户编号
     * @param tankId 鱼缸编号
     * @param requestedPage 客户端页码
     * @param requestedSize 客户端每页条数
     * @return 当前鱼缸的生物分页
     */
    public ReefPageResponse<ReefBiologyDto> list(Long userId, Long tankId, Integer requestedPage, Integer requestedSize) {
        tankService.ensureOwned(userId, tankId);
        int pageNum = ReefPageSupport.pageNumber(requestedPage);
        int pageSize = ReefPageSupport.pageSize(requestedSize);
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reef_biology WHERE user_id = ? AND tank_id = ?", Long.class, userId, tankId);
        List<ReefBiologyDto> rows = jdbcTemplate.query(selectBiology() + " WHERE user_id = ? AND tank_id = ? ORDER BY added_at DESC, id DESC LIMIT ? OFFSET ?",
                BeanPropertyRowMapper.newInstance(ReefBiologyDto.class), userId, tankId, pageSize, ReefPageSupport.offset(pageNum, pageSize));
        decorateBiology(rows);
        return ReefPageSupport.response(pageNum, pageSize, total == null ? ZERO : total, rows);
    }

    /**
     * 读取当前用户拥有的单个生物详情。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 认证上下文中的用户编号
     * @param biologyId 生物编号
     * @return 生物详情
     */
    public ReefBiologyDto get(Long userId, Long biologyId) {
        List<ReefBiologyDto> rows = jdbcTemplate.query(selectBiology() + " WHERE user_id = ? AND id = ? LIMIT 1",
                BeanPropertyRowMapper.newInstance(ReefBiologyDto.class), userId, biologyId);
        if (rows.isEmpty()) throw new ReefJournalException("没有找到这条生物记录，或它不属于当前账号");
        ReefBiologyDto biology = rows.get(ZERO);
        decorateBiology(biology);
        return biology;
    }

    /**
     * 新增生物时建立入缸事件，更新状态时记录状态变化事件。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 认证上下文中的用户编号
     * @param request 已校验的生物资料
     * @return 保存后的生物详情
     */
    @Transactional
    public ReefBiologyDto save(Long userId, ReefBiologySaveRequest request) {
        Long tankId = tankService.parseId(request.getTankId());
        tankService.ensureOwned(userId, tankId);
        validateCategory(request.getCategory());
        LocalDate addedAt = ReefDateTimeSupport.parseDate(request.getAddedAt());
        String status = isBlank(request.getStatus()) ? BIOLOGY_STATUS_NORMAL : request.getStatus();
        validateStatus(status);
        if (isBlank(request.getId())) {
            Long biologyId = insertBiology(userId, tankId, request, addedAt, status);
            insertEvent(userId, biologyId, addedAt.atStartOfDay(), "入缸", "生物入缸", status);
            return get(userId, biologyId);
        }
        Long biologyId = tankService.parseId(request.getId());
        String previousStatus = currentStatus(userId, biologyId);
        jdbcTemplate.update("UPDATE reef_biology SET tank_id = ?, name = ?, scientific_name = ?, category = ?, added_at = ?, body_length_cm = ?, source = ?, status = ?, updated_at = NOW() WHERE user_id = ? AND id = ?",
                tankId, request.getName().trim(), request.getScientificName(), request.getCategory(), addedAt,
                request.getBodyLengthCm(), request.getSource(), status, userId, biologyId);
        if (!status.equals(previousStatus)) {
            insertEvent(userId, biologyId, ReefDateTimeSupport.now(), "状态更新", "生物状态已更新", status);
        }
        return get(userId, biologyId);
    }

    /**
     * 按用户和生物编号读取事件时间轴。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 认证上下文中的用户编号
     * @param biologyId 生物编号
     * @return 按发生时间正序排列的事件集合
     */
    public List<ReefBiologyEventDto> events(Long userId, Long biologyId) {
        get(userId, biologyId);
        return jdbcTemplate.query("SELECT CAST(id AS CHAR) id, DATE_FORMAT(event_at, '%Y-%m-%d %H:%i:%s') event_at, title, description, status, status status_key FROM reef_biology_event WHERE user_id = ? AND biology_id = ? ORDER BY event_at ASC, id ASC",
                BeanPropertyRowMapper.newInstance(ReefBiologyEventDto.class), userId, biologyId);
    }

    /**
     * 一次读取当前用户全部事件，供用户级数据导出使用。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 当前用户编号
     * @return 当前用户生物事件集合
     */
    public List<ReefBiologyEventDto> eventsForUser(Long userId) {
        return jdbcTemplate.query("SELECT CAST(id AS CHAR) id, DATE_FORMAT(event_at, '%Y-%m-%d %H:%i:%s') event_at, title, description, status, status status_key FROM reef_biology_event WHERE user_id = ? ORDER BY event_at ASC, id ASC",
                BeanPropertyRowMapper.newInstance(ReefBiologyEventDto.class), userId);
    }

    /**
     * 新增当前用户生物事件并返回详情时间轴。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 认证上下文中的用户编号
     * @param biologyId 生物编号
     * @param request 已校验事件内容
     * @return 生物事件列表
     */
    @Transactional
    public List<ReefBiologyEventDto> saveEvent(Long userId, Long biologyId, ReefBiologyEventSaveRequest request) {
        get(userId, biologyId);
        LocalDateTime eventAt = ReefDateTimeSupport.parseDateTime(request.getEventAt());
        insertEvent(userId, biologyId, eventAt, request.getTitle().trim(), request.getDescription(), request.getStatus());
        return events(userId, biologyId);
    }

    /**
     * 写入新生物资料并返回数据库生成的主键。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 所属用户编号
     * @param tankId 已校验鱼缸编号
     * @param request 生物资料请求
     * @param addedAt 入缸日期
     * @param status 用户选择的生物状态
     * @return 新生物编号
     */
    private Long insertBiology(Long userId, Long tankId, ReefBiologySaveRequest request, LocalDate addedAt, String status) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update((Connection connection) -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO reef_biology (user_id, tank_id, name, scientific_name, category, added_at, body_length_cm, source, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(ONE, userId);
            statement.setLong(2, tankId);
            statement.setString(3, request.getName().trim());
            statement.setString(4, request.getScientificName());
            statement.setString(5, request.getCategory());
            statement.setObject(6, addedAt);
            statement.setBigDecimal(7, request.getBodyLengthCm());
            statement.setString(8, request.getSource());
            statement.setString(9, status);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) throw new ReefJournalException("生物资料没有保存，请重试");
        return key.longValue();
    }

    /**
     * 将入缸、状态更新或用户补充事件写入时间轴。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 所属用户编号
     * @param biologyId 所属生物编号
     * @param eventAt 事件时间
     * @param title 事件标题
     * @param description 用户补充描述
     * @param status 关联状态
     */
    private void insertEvent(Long userId, Long biologyId, LocalDateTime eventAt, String title, String description, String status) {
        jdbcTemplate.update("INSERT INTO reef_biology_event (user_id, biology_id, event_at, title, description, status, created_at) VALUES (?, ?, ?, ?, ?, ?, NOW())",
                userId, biologyId, eventAt, title, description, status);
    }

    /**
     * 获取当前用户生物的已保存状态，用于判断是否追加状态事件。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 当前用户编号
     * @param biologyId 生物编号
     * @return 当前状态；记录不存在时抛出业务异常
     */
    private String currentStatus(Long userId, Long biologyId) {
        List<String> values = jdbcTemplate.query("SELECT status FROM reef_biology WHERE user_id = ? AND id = ? LIMIT 1",
                (resultSet, rowNumber) -> resultSet.getString(1), userId, biologyId);
        if (values.isEmpty()) throw new ReefJournalException("没有找到这条生物记录，或它不属于当前账号");
        return values.get(ZERO);
    }

    /**
     * 返回生物列表所需固定字段投影。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @return 参数化查询前缀
     */
    private String selectBiology() {
        return "SELECT CAST(id AS CHAR) id, CAST(tank_id AS CHAR) tank_id, name, scientific_name, category, DATE_FORMAT(added_at, '%Y-%m-%d') added_at, CAST(body_length_cm AS CHAR) body_length_cm, source, status, status status_key FROM reef_biology";
    }

    /**
     * 批量计算页面展示所需的入缸天数和状态文案。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param rows 当前页生物记录
     */
    private void decorateBiology(List<ReefBiologyDto> rows) {
        for (ReefBiologyDto row : rows) decorateBiology(row);
    }

    /**
     * 补齐单条生物详情的自然日数和状态标签。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param row 生物详情响应
     */
    private void decorateBiology(ReefBiologyDto row) {
        row.setDaysInTank((int) java.time.temporal.ChronoUnit.DAYS.between(ReefDateTimeSupport.parseDate(row.getAddedAt()), LocalDate.now(ZoneId.of(TIME_ZONE))));
        row.setStatusLabel(statusLabel(row.getStatus()));
    }

    /**
     * 将固定生物状态编码转换为中文标签。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param status 生物状态编码
     * @return 中文状态标签
     */
    private String statusLabel(String status) {
        if (BIOLOGY_STATUS_NORMAL.equals(status)) return "正常";
        if (BIOLOGY_STATUS_QUARANTINE.equals(status)) return "检疫中";
        if (BIOLOGY_STATUS_ISOLATED.equals(status)) return "隔离中";
        if (BIOLOGY_STATUS_SICK.equals(status)) return "生病";
        if (BIOLOGY_STATUS_DEAD.equals(status)) return "阵亡";
        return "待补充";
    }

    /**
     * 校验生物分类只使用当前页面提供的选项。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param category 用户提交的生物分类
     */
    private void validateCategory(String category) {
        if (!java.util.Arrays.asList(CATEGORY_FISH, CATEGORY_CORAL, CATEGORY_PLANT, CATEGORY_OTHER).contains(category)) {
            throw new ReefJournalException("生物分类不受支持");
        }
    }

    /**
     * 校验生物状态只使用业务约定的五种状态。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param status 用户提交的生物状态
     */
    private void validateStatus(String status) {
        if (!java.util.Arrays.asList(BIOLOGY_STATUS_NORMAL, BIOLOGY_STATUS_QUARANTINE, BIOLOGY_STATUS_ISOLATED,
                BIOLOGY_STATUS_SICK, BIOLOGY_STATUS_DEAD).contains(status)) {
            throw new ReefJournalException("生物状态不受支持");
        }
    }

    /**
     * 判断可选文本是否为空白。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param value 待判断文本
     * @return 文本为空或仅包含空白字符时返回 true
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
