package com.tiankong44.tool.reefjournal.service;

import com.tiankong44.tool.reefjournal.client.ReefWechatClient;
import com.tiankong44.tool.reefjournal.config.ReefJournalProperties;
import com.tiankong44.tool.reefjournal.dto.ReefEnabledReminderDto;
import com.tiankong44.tool.reefjournal.dto.ReefReminderDeliveryDto;
import com.tiankong44.tool.reefjournal.dto.ReefReminderEquipmentDto;
import com.tiankong44.tool.reefjournal.dto.ReefReminderLastLogDto;
import com.tiankong44.tool.reefjournal.exception.ReefJournalException;
import com.tiankong44.tool.reefjournal.support.ReefDateTimeSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.tiankong44.tool.reefjournal.ReefJournalConstants.FEEDING_INTERVAL_DAYS;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.DATE_PATTERN;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.TIME_PATTERN;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.ONE;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.REDIS_LOCK_RELEASE_SCRIPT;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.REDIS_REMINDER_LOCK;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.STATUS_FAILED;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.STATUS_CANCELLED;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.STATUS_PENDING;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.STATUS_SENT;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.STATUS_SENDING;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.STATUS_WAITING_CONSENT;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.TYPE_EQUIPMENT;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.TYPE_FEEDING;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.TYPE_WATER_CHANGE;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.TYPE_WATER_TEST;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.WATER_CHANGE_INTERVAL_DAYS;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.WATER_TEST_INTERVAL_DAYS;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.ZERO;

/**
 * 使用共享 Redis 租约和唯一任务编号派发订阅提醒。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Slf4j
@Service
public class ReefReminderDispatchService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(TIME_PATTERN);
    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ReefJournalProperties properties;
    private final ReefWechatClient wechatClient;
    private final ReefReminderService reminderService;

    /**
     * 创建共享 Redis 锁保护的提醒派发服务。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param jdbcTemplate Spring 管理的数据库访问器
     * @param redisTemplate 共享 Redis 操作器
     * @param properties 调度锁和消息配置
     * @param wechatClient 微信订阅消息客户端
     * @param reminderService 用户提醒和授权服务
     */
    public ReefReminderDispatchService(JdbcTemplate jdbcTemplate, StringRedisTemplate redisTemplate,
                                      ReefJournalProperties properties, ReefWechatClient wechatClient, ReefReminderService reminderService) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.wechatClient = wechatClient;
        this.reminderService = reminderService;
    }

    /**
     * 使用 Redis SET NX 租约限制多实例并发，并在 HTTP 调用前完成任务状态登记。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     */
    public void dispatchDue() {
        String lockToken = UUID.randomUUID().toString();
        Long leaseSeconds = properties.getReminder().getLockLeaseSeconds();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(REDIS_REMINDER_LOCK, lockToken, leaseSeconds, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(acquired)) return;
        try {
            LocalDateTime now = ReefDateTimeSupport.now();
            registerDueTasks(now);
            sendPendingTasks(now);
        } catch (RuntimeException exception) {
            log.warn("海缸提醒调度未能完成本轮处理", exception);
        } finally {
            releaseLock(lockToken);
        }
    }

    /**
     * 收集日志周期和设备维护周期到期的提醒任务。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param now 当前上海本地时间
     */
    private void registerDueTasks(LocalDateTime now) {
        Map<String, String> sendTimes = readSendTimes();
        registerLogTasks(now, sendTimes);
        registerEquipmentTasks(now, sendTimes);
    }

    /**
     * 分别读取提醒开关和统一发送时刻，并在 Java 中按用户关联。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @return 用户编号与提醒类型到发送时刻的索引
     */
    private Map<String, String> readSendTimes() {
        List<ReefEnabledReminderDto> enabled = jdbcTemplate.query("SELECT user_id, type FROM reef_reminder_setting WHERE enabled = ?",
                BeanPropertyRowMapper.newInstance(ReefEnabledReminderDto.class), ONE);
        List<Map<String, Object>> preferences = jdbcTemplate.queryForList("SELECT user_id, TIME_FORMAT(send_time, '%H:%i') send_time FROM reef_reminder_preference WHERE send_time IS NOT NULL");
        Map<String, String> values = new HashMap<>();
        for (Map<String, Object> preference : preferences) {
            values.put(String.valueOf(preference.get("user_id")), String.valueOf(preference.get("send_time")));
        }
        Map<String, String> active = new HashMap<>();
        for (ReefEnabledReminderDto item : enabled) {
            String sendTime = values.get(String.valueOf(item.getUserId()));
            if (sendTime != null) active.put(key(item.getUserId(), item.getType()), sendTime);
        }
        return active;
    }

    /**
     * 按最近一条换水、水质或喂食日志计算周期提醒。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param now 当前上海本地时间
     * @param sendTimes 用户统一提醒时刻
     */
    private void registerLogTasks(LocalDateTime now, Map<String, String> sendTimes) {
        List<ReefReminderLastLogDto> logs = jdbcTemplate.query(
                "SELECT user_id, type, DATE_FORMAT(MAX(recorded_at), '%Y-%m-%d %H:%i:%s') last_at FROM reef_log WHERE type IN (?, ?, ?) GROUP BY user_id, type",
                BeanPropertyRowMapper.newInstance(ReefReminderLastLogDto.class), TYPE_WATER_CHANGE, TYPE_WATER_TEST, TYPE_FEEDING);
        for (ReefReminderLastLogDto logItem : logs) {
            Integer interval = reminderService.intervalDays(logItem.getType());
            String sendTime = sendTimes.get(key(logItem.getUserId(), logItem.getType()));
            if (interval == null || sendTime == null) continue;
            LocalDate dueDate = ReefDateTimeSupport.parseDateTime(logItem.getLastAt()).toLocalDate().plusDays(interval);
            if (isDue(now, dueDate, sendTime)) {
                registerTask(logItem.getUserId(), logItem.getType(), null, dueDate, sendTime);
            }
        }
    }

    /**
     * 按设备的上次维护日期和逐台维护周期生成提醒任务。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param now 当前上海本地时间
     * @param sendTimes 用户统一提醒时刻
     */
    private void registerEquipmentTasks(LocalDateTime now, Map<String, String> sendTimes) {
        String sql = "SELECT id, user_id, name, DATE_FORMAT(last_maintained_at, '%Y-%m-%d') last_maintained_at, maintenance_interval_days FROM reef_equipment WHERE last_maintained_at IS NOT NULL AND maintenance_interval_days > ?";
        List<ReefReminderEquipmentDto> equipment = jdbcTemplate.query(sql, BeanPropertyRowMapper.newInstance(ReefReminderEquipmentDto.class), ZERO);
        for (ReefReminderEquipmentDto item : equipment) {
            String sendTime = sendTimes.get(key(item.getUserId(), TYPE_EQUIPMENT));
            if (sendTime == null) continue;
            LocalDate dueDate = ReefDateTimeSupport.parseDate(item.getLastMaintainedAt()).plusDays(item.getMaintenanceIntervalDays());
            if (isDue(now, dueDate, sendTime)) {
                registerTask(item.getUserId(), TYPE_EQUIPMENT, item.getId(), dueDate, sendTime);
            }
        }
    }

    /**
     * 使用用户、提醒类型、关联设备和周期日期组成唯一任务编号。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 所属用户编号
     * @param type 提醒类型
     * @param entityId 可选设备编号
     * @param cycleDueDate 周期到期日期
     * @param sendTime 用户设置的发送时刻
     */
    private void registerTask(Long userId, String type, Long entityId, LocalDate cycleDueDate, String sendTime) {
        String taskKey = userId + ":" + type + ":" + (entityId == null ? "0" : entityId) + ":" + cycleDueDate.format(DATE_FORMATTER);
        LocalDateTime deliveryAt = LocalDateTime.of(ReefDateTimeSupport.now().toLocalDate(), LocalTime.parse(sendTime, TIME_FORMATTER));
        try {
            jdbcTemplate.update("INSERT INTO reef_reminder_delivery (task_key, user_id, type, entity_id, due_at, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                    taskKey, userId, type, entityId, deliveryAt, STATUS_PENDING);
        } catch (DuplicateKeyException exception) {
            // 唯一任务编号已存在，避免同一提醒周期重复排队。
        }
    }

    /**
     * 读取有限批次待发送任务，并逐项更新授权额度和任务状态。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param now 当前上海本地时间
     */
    private void sendPendingTasks(LocalDateTime now) {
        List<ReefReminderDeliveryDto> pending = jdbcTemplate.query(
                "SELECT task_key, user_id, type, entity_id, DATE_FORMAT(due_at, '%Y-%m-%d %H:%i:%s') due_at FROM reef_reminder_delivery WHERE status = ? AND due_at <= ? ORDER BY due_at ASC LIMIT ?",
                BeanPropertyRowMapper.newInstance(ReefReminderDeliveryDto.class), STATUS_PENDING, now, 100);
        for (ReefReminderDeliveryDto delivery : pending) sendOne(delivery);
    }

    /**
     * 检查开关和额度后向微信发送一条订阅消息。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param delivery 已登记的提醒任务
     */
    private void sendOne(ReefReminderDeliveryDto delivery) {
        if (!reminderService.isEnabled(delivery.getUserId(), delivery.getType())) {
            updateDelivery(delivery.getTaskKey(), STATUS_CANCELLED, "用户已关闭该类提醒");
            return;
        }
        if (!wechatClient.isTemplateReady(delivery.getType())) {
            updateDelivery(delivery.getTaskKey(), STATUS_FAILED, "订阅消息模板配置待补充");
            return;
        }
        int claimed = jdbcTemplate.update("UPDATE reef_reminder_delivery SET status = ?, updated_at = NOW() WHERE task_key = ? AND status = ?",
                STATUS_SENDING, delivery.getTaskKey(), STATUS_PENDING);
        if (claimed != ONE) return;
        int reserved = jdbcTemplate.update("UPDATE reef_subscription_quota SET available_count = available_count - ? , updated_at = NOW() WHERE user_id = ? AND type = ? AND available_count > ?",
                ONE, delivery.getUserId(), delivery.getType(), ZERO);
        if (reserved != ONE) {
            updateDelivery(delivery.getTaskKey(), STATUS_WAITING_CONSENT, "用户尚未提供可用订阅额度");
            return;
        }
        try {
            String openid = jdbcTemplate.queryForObject("SELECT openid FROM reef_user WHERE id = ?", String.class, delivery.getUserId());
            // 微信请求在数据库更新完成后执行，不占用事务连接等待外部服务。
            boolean accepted = wechatClient.sendReminder(openid, delivery.getType(), reminderTitle(delivery), delivery.getDueAt());
            updateDelivery(delivery.getTaskKey(), accepted ? STATUS_SENT : STATUS_FAILED,
                    accepted ? null : "微信未接收本次提醒");
        } catch (RuntimeException exception) {
            updateDelivery(delivery.getTaskKey(), STATUS_FAILED, "微信提醒发送失败，请重新授权后继续使用");
            log.warn("海缸订阅提醒发送失败，任务编号：{}", delivery.getTaskKey());
        }
    }

    /**
     * 持久化单次订阅发送结果。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param taskKey 唯一任务编号
     * @param status 新任务状态
     * @param error 失败说明；成功时为空
     */
    private void updateDelivery(String taskKey, String status, String error) {
        jdbcTemplate.update("UPDATE reef_reminder_delivery SET status = ?, last_error = ?, updated_at = NOW() WHERE task_key = ?",
                status, error, taskKey);
    }

    /**
     * 仅由当前持有者释放 Redis 租约。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param lockToken 当前调度执行的随机持有标识
     */
    private void releaseLock(String lockToken) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(REDIS_LOCK_RELEASE_SCRIPT);
        script.setResultType(Long.class);
        // 仅锁持有者可释放自己的租约，避免误删其他实例刚取得的锁。
        redisTemplate.execute(script, Arrays.asList(REDIS_REMINDER_LOCK), lockToken);
    }

    /**
     * 判断周期日期和用户设定时刻是否均已到达。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param now 当前上海本地时间
     * @param dueDate 周期到期日
     * @param sendTime 用户设置的统一发送时刻
     * @return 到期且已达到发送时刻时返回 true
     */
    private boolean isDue(LocalDateTime now, LocalDate dueDate, String sendTime) {
        return !dueDate.isAfter(now.toLocalDate()) && !now.toLocalTime().isBefore(LocalTime.parse(sendTime, TIME_FORMATTER));
    }

    /**
     * 生成各类提醒的中文标题内容。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param delivery 已登记的提醒任务
     * @return 对应类别的通知标题
     */
    private String reminderTitle(ReefReminderDeliveryDto delivery) {
        if (TYPE_WATER_CHANGE.equals(delivery.getType())) return "鱼缸到了换水提醒时间";
        if (TYPE_WATER_TEST.equals(delivery.getType())) return "记得检测鱼缸水质";
        if (TYPE_FEEDING.equals(delivery.getType())) return "今天的喂食记录还没更新";
        return "设备维护时间到了";
    }

    /**
     * 生成仅用于内存装配的用户提醒查找键。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 用户编号
     * @param type 提醒类型
     * @return 组合查找键
     */
    private String key(Long userId, String type) {
        return userId + ":" + type;
    }
}
