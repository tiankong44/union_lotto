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
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.BIOLOGY_STATUS_OBSERVING;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.BIOLOGY_STATUS_GROWING;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.BIOLOGY_STATUS_POOR;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.BIOLOGY_STATUS_CLOSED;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.BIOLOGY_STATUS_FADED;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.CATEGORY_FISH;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.CATEGORY_CORAL;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.CATEGORY_PLANT;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.CATEGORY_OTHER;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.TIME_ZONE;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.ZERO;
import static com.tiankong44.tool.reefjournal.ReefAquariumConstants.BIOLOGY_CATEGORIES;
import static com.tiankong44.tool.reefjournal.ReefAquariumConstants.BIOLOGY_EVENTS;
import static com.tiankong44.tool.reefjournal.ReefAquariumConstants.EVENT_TRANSFER;
import static com.tiankong44.tool.reefjournal.ReefAquariumConstants.TRACKING_SINGLE;
import static com.tiankong44.tool.reefjournal.ReefAquariumConstants.TRACKING_BATCH;

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
            // 新档案缺省按单个生物记录；已有档案由下面的分支保留原值。
            if (request.getTrackingMode() == null) request.setTrackingMode(TRACKING_SINGLE);
            if (request.getQuantity() == null) request.setQuantity(ONE);
            if (request.getCurrentTankSince() == null) request.setCurrentTankSince(request.getAddedAt());
            validateTracking(request.getTrackingMode(), request.getQuantity());
            Long biologyId = insertBiology(userId, tankId, request, addedAt, status);
            insertEvent(userId, biologyId, addedAt.atStartOfDay(), "入缸", "生物入缸", status);
            return get(userId, biologyId);
        }
        Long biologyId = tankService.parseId(request.getId());
        // 读取原档案，避免旧客户端更新时覆盖新增字段，并保留转缸前的归属。
        ReefBiologyDto previous = lockBiology(userId, biologyId);
        if (request.getTrackingMode() == null) request.setTrackingMode(previous.getTrackingMode());
        if (request.getQuantity() == null) request.setQuantity(previous.getQuantity());
        validateTracking(request.getTrackingMode(), request.getQuantity());
        jdbcTemplate.update("UPDATE reef_biology SET tank_id = ?, name = ?, scientific_name = ?, category = ?, added_at = ?, body_length_cm = ?, source = ?, status = ?, tracking_mode = ?, quantity = ?, position_note = COALESCE(?, position_note), note = COALESCE(?, note), updated_at = NOW() WHERE user_id = ? AND id = ?",
                tankId, request.getName().trim(), request.getScientificName(), request.getCategory(), addedAt,
                request.getBodyLengthCm(), request.getSource(), status, request.getTrackingMode(), request.getQuantity(),
                request.getPositionNote(), request.getNote(), userId, biologyId);
        if (!status.equals(previous.getStatus())) {
            insertEvent(userId, biologyId, ReefDateTimeSupport.now(), "状态更新", "生物状态已更新", status);
        }
        if (!String.valueOf(tankId).equals(previous.getTankId())) {
            // 资料更新发生转缸时同样写入原缸与目标缸，避免丢失历史归属。
            jdbcTemplate.update("UPDATE reef_biology SET current_tank_since = ? WHERE user_id = ? AND id = ?", ReefDateTimeSupport.now().toLocalDate(), userId, biologyId);
            insertDetailedEvent(userId, biologyId, ReefDateTimeSupport.now(), "转缸", null, status, EVENT_TRANSFER,
                    Long.valueOf(previous.getTankId()), tankId, request.getQuantity(), null);
        }
        return get(userId, biologyId);
    }

    /**
     * 恢复生物当前资料；历史事件由备份独立恢复，避免重复生成入缸事件。
     * @Author zhanghao_SMEICS
     * @Date 2026-11-12
     */
    public ReefBiologyDto restoreNew(Long userId, ReefBiologySaveRequest request) {
        Long tankId = tankService.parseId(request.getTankId());
        // 只允许将备份记录恢复到当前账号已恢复的鱼缸。
        tankService.ensureOwned(userId, tankId);
        validateCategory(request.getCategory());
        String status = isBlank(request.getStatus()) ? BIOLOGY_STATUS_NORMAL : request.getStatus();
        validateStatus(status);
        if (request.getTrackingMode() == null) request.setTrackingMode(TRACKING_SINGLE);
        if (request.getQuantity() == null) request.setQuantity(ONE);
        validateTracking(request.getTrackingMode(), request.getQuantity());
        LocalDate addedAt = ReefDateTimeSupport.parseDate(request.getAddedAt());
        Long biologyId = insertBiology(userId, tankId, request, addedAt, status);
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
        return jdbcTemplate.query(eventSelect() + " WHERE user_id = ? AND biology_id = ? ORDER BY event_at ASC, id ASC",
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
        return jdbcTemplate.query(eventSelect() + " WHERE user_id = ? ORDER BY event_at ASC, id ASC",
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
        // 保留原接口的事件列表响应，供旧客户端继续使用。
        createEvent(userId, biologyId, request);
        return events(userId, biologyId);
    }

    /**
     * 保存一条用户事件并返回编号，供照片可靠关联到该事件。
     * @Author zhanghao_SMEICS
     * @Date 2026-11-12
     */
    @Transactional
    public ReefBiologyEventDto createEvent(Long userId, Long biologyId, ReefBiologyEventSaveRequest request) {
        // 校验生物归属，并以事件发生前的鱼缸作为历史归属。
        ReefBiologyDto biology = lockBiology(userId, biologyId);
        LocalDateTime eventAt = ReefDateTimeSupport.parseDateTime(request.getEventAt());
        String eventType = isBlank(request.getEventType()) ? "OBSERVATION" : request.getEventType();
        if (!BIOLOGY_EVENTS.contains(eventType)) throw new ReefJournalException("生物事件类型不受支持");
        if (request.getQuantity() != null && request.getQuantity() < ZERO) throw new ReefJournalException("事件数量不能小于零");
        Long fromTankId = Long.valueOf(biology.getTankId());
        Long toTankId = null;
        Long relatedBiologyId = null;
        Integer eventQuantity = request.getQuantity();
        if (EVENT_TRANSFER.equals(eventType)) {
            // 仅允许转入当前用户拥有的其他鱼缸。
            if (isBlank(request.getToTankId())) throw new ReefJournalException("请选择转入的鱼缸");
            toTankId = tankService.parseId(request.getToTankId());
            if (Boolean.TRUE.equals(tankService.get(userId, toTankId).getArchived())) throw new ReefJournalException("请先恢复归档的目标鱼缸");
            if (toTankId.equals(fromTankId)) throw new ReefJournalException("转入鱼缸与当前鱼缸相同");
            eventQuantity = eventQuantity == null ? biology.getQuantity() : eventQuantity;
            if (eventQuantity == null || eventQuantity <= ZERO || eventQuantity > biology.getQuantity()) throw new ReefJournalException("转缸数量需大于零且不能超过当前数量");
            if (eventQuantity < biology.getQuantity()) {
                // 批次部分转缸时保留原批次并为目标缸创建关联批次。
                if (!TRACKING_BATCH.equals(biology.getTrackingMode())) throw new ReefJournalException("个体记录不能部分转缸");
                relatedBiologyId = splitBatch(userId, biology, toTankId, eventAt, eventQuantity, request.getStatus());
                insertDetailedEvent(userId, relatedBiologyId, eventAt, "转入", request.getDescription(), request.getStatus(), eventType,
                        fromTankId, toTankId, eventQuantity, biologyId);
            } else {
                jdbcTemplate.update("UPDATE reef_biology SET tank_id = ?, current_tank_since = ?, updated_at = NOW() WHERE user_id = ? AND id = ?", toTankId, eventAt.toLocalDate(), userId, biologyId);
            }
        }
        if (!isBlank(request.getStatus())) {
            // 只有用户明确选择状态时才改变当前状态。
            validateStatus(request.getStatus());
            jdbcTemplate.update("UPDATE reef_biology SET status = ?, updated_at = NOW() WHERE user_id = ? AND id = ?", request.getStatus(), userId, relatedBiologyId == null ? biologyId : relatedBiologyId);
        }
        Long eventId = insertDetailedEvent(userId, biologyId, eventAt, request.getTitle().trim(), request.getDescription(),
                relatedBiologyId == null ? request.getStatus() : null, eventType, fromTankId, toTankId, eventQuantity, relatedBiologyId);
        // 以刚生成的字符串编号定位事件，避免并发时把照片关联到其他事件。
        List<ReefBiologyEventDto> saved = jdbcTemplate.query(eventSelect() + " WHERE user_id = ? AND biology_id = ? AND id = ?",
                BeanPropertyRowMapper.newInstance(ReefBiologyEventDto.class), userId, biologyId, eventId);
        if (saved.isEmpty()) throw new ReefJournalException("生物事件已保存，但暂时无法读取");
        return saved.get(ZERO);
    }

    /** 拆分批次并保留原批次数量，目标批次从转入当天开始记录。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    private Long splitBatch(Long userId, ReefBiologyDto source, Long targetTankId, LocalDateTime eventAt, int quantity, String selectedStatus) {
        ReefBiologySaveRequest request = new ReefBiologySaveRequest();
        request.setName(source.getName()); request.setScientificName(source.getScientificName()); request.setCategory(source.getCategory());
        request.setAddedAt(eventAt.toLocalDate().toString()); request.setCurrentTankSince(request.getAddedAt());
        request.setBodyLengthCm(source.getBodyLengthCm() == null ? null : new java.math.BigDecimal(source.getBodyLengthCm()));
        request.setSource(source.getSource()); request.setTrackingMode(TRACKING_BATCH); request.setQuantity(quantity);
        request.setPositionNote(source.getPositionNote()); request.setNote(source.getNote());
        String status = isBlank(selectedStatus) ? source.getStatus() : selectedStatus;
        validateStatus(status);
        Long newId = insertBiology(userId, targetTankId, request, eventAt.toLocalDate(), status);
        jdbcTemplate.update("UPDATE reef_biology SET quantity = ?, updated_at = NOW() WHERE user_id = ? AND id = ?",
                source.getQuantity() - quantity, userId, Long.valueOf(source.getId()));
        return newId;
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
                    "INSERT INTO reef_biology (user_id, tank_id, name, scientific_name, category, added_at, body_length_cm, source, status, tracking_mode, quantity, position_note, note, current_tank_since, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
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
            statement.setString(10, request.getTrackingMode());
            statement.setInt(11, request.getQuantity());
            statement.setString(12, request.getPositionNote());
            statement.setString(13, request.getNote());
            statement.setObject(14, isBlank(request.getCurrentTankSince()) ? null : ReefDateTimeSupport.parseDate(request.getCurrentTankSince()));
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
     * 写入带鱼缸归属的用户事件，保留转缸前后的历史关系。
     * @Author zhanghao_SMEICS
     * @Date 2026-11-12
     */
    private Long insertDetailedEvent(Long userId, Long biologyId, LocalDateTime eventAt, String title, String description,
                                     String status, String type, Long fromTankId, Long toTankId, Integer quantity, Long relatedBiologyId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO reef_biology_event (user_id, biology_id, event_at, title, description, status, event_type, from_tank_id, to_tank_id, quantity, related_biology_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())", Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, userId);
            statement.setLong(2, biologyId);
            statement.setObject(3, eventAt);
            statement.setString(4, title);
            statement.setString(5, description);
            statement.setString(6, status);
            statement.setString(7, type);
            statement.setObject(8, fromTankId);
            statement.setObject(9, toTankId);
            statement.setObject(10, quantity);
            statement.setObject(11, relatedBiologyId);
            return statement;
        }, keyHolder);
        Number id = keyHolder.getKey();
        if (id == null) throw new ReefJournalException("生物事件没有保存，请重试");
        return id.longValue();
    }

    /** 锁定当前用户生物，避免并发转缸或状态更新覆盖历史。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    private ReefBiologyDto lockBiology(Long userId, Long biologyId) {
        List<Long> rows = jdbcTemplate.query("SELECT id FROM reef_biology WHERE user_id = ? AND id = ? FOR UPDATE",
                (resultSet, rowNumber) -> resultSet.getLong(1), userId, biologyId);
        if (rows.isEmpty()) throw new ReefJournalException("没有找到这条生物记录，或它不属于当前账号");
        return get(userId, biologyId);
    }

    /** 统一事件响应字段，保留拆分前后的生物关系。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    private String eventSelect() {
        return "SELECT CAST(id AS CHAR) id, CAST(biology_id AS CHAR) biology_id, event_type, CAST(from_tank_id AS CHAR) from_tank_id, CAST(to_tank_id AS CHAR) to_tank_id, CAST(related_biology_id AS CHAR) related_biology_id, quantity, DATE_FORMAT(event_at, '%Y-%m-%d %H:%i:%s') event_at, title, description, status, status status_key FROM reef_biology_event";
    }

    /**
     * 返回生物列表所需固定字段投影。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @return 参数化查询前缀
     */
    private String selectBiology() {
        return "SELECT CAST(id AS CHAR) id, CAST(tank_id AS CHAR) tank_id, name, scientific_name, category, DATE_FORMAT(added_at, '%Y-%m-%d') added_at, DATE_FORMAT(current_tank_since, '%Y-%m-%d') current_tank_since, CAST(body_length_cm AS CHAR) body_length_cm, source, status, status status_key, tracking_mode, quantity, position_note, note FROM reef_biology";
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
        String enteredAt = row.getCurrentTankSince() == null ? row.getAddedAt() : row.getCurrentTankSince();
        row.setDaysInTank((int) java.time.temporal.ChronoUnit.DAYS.between(ReefDateTimeSupport.parseDate(enteredAt), LocalDate.now(ZoneId.of(TIME_ZONE))));
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
        if (BIOLOGY_STATUS_DEAD.equals(status)) return "死亡";
        if (BIOLOGY_STATUS_OBSERVING.equals(status)) return "待观察";
        if (BIOLOGY_STATUS_GROWING.equals(status)) return "生长中";
        if (BIOLOGY_STATUS_POOR.equals(status)) return "状态欠佳";
        if (BIOLOGY_STATUS_CLOSED.equals(status)) return "收缩";
        if (BIOLOGY_STATUS_FADED.equals(status)) return "颜色变浅";
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
        if (!BIOLOGY_CATEGORIES.contains(category)) {
            throw new ReefJournalException("生物分类不受支持");
        }
    }

    /**
     * 校验个体或批次的记录方式及当前数量。
     * @Author zhanghao_SMEICS
     * @Date 2026-11-12
     */
    private void validateTracking(String mode, Integer quantity) {
        if (!TRACKING_SINGLE.equals(mode) && !TRACKING_BATCH.equals(mode)) throw new ReefJournalException("请选择个体或批次记录");
        if (quantity == null || quantity < ZERO) throw new ReefJournalException("当前数量不能小于零");
        if (TRACKING_SINGLE.equals(mode) && quantity > ONE) throw new ReefJournalException("个体记录的数量不能超过1");
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
                BIOLOGY_STATUS_SICK, BIOLOGY_STATUS_DEAD, BIOLOGY_STATUS_OBSERVING, BIOLOGY_STATUS_GROWING,
                BIOLOGY_STATUS_POOR, BIOLOGY_STATUS_CLOSED, BIOLOGY_STATUS_FADED).contains(status)) {
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
