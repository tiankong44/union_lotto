package com.tiankong44.tool.reefjournal.service;

import com.tiankong44.tool.reefjournal.config.ReefJournalProperties;
import com.tiankong44.tool.reefjournal.dto.ReefReminderItemDto;
import com.tiankong44.tool.reefjournal.dto.ReefReminderSettingsDto;
import com.tiankong44.tool.reefjournal.dto.ReefSubscriptionConsentRequest;
import com.tiankong44.tool.reefjournal.exception.ReefJournalException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.tiankong44.tool.reefjournal.ReefJournalConstants.DEFAULT_EQUIPMENT_ENABLED;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.DEFAULT_FEEDING_ENABLED;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.DEFAULT_WATER_CHANGE_ENABLED;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.DEFAULT_WATER_TEST_ENABLED;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.FEEDING_INTERVAL_DAYS;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.ONE;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.STATUS_PENDING;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.STATUS_CANCELLED;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.STATUS_WAITING_CONSENT;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.TYPE_EQUIPMENT;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.TYPE_FEEDING;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.TYPE_WATER_CHANGE;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.TYPE_WATER_TEST;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.WATER_CHANGE_INTERVAL_DAYS;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.WATER_TEST_INTERVAL_DAYS;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.ZERO;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.TIME_ZONE;

/**
 * 保存用户提醒偏好、提醒开关及订阅消息授权额度。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Service
public class ReefReminderService {
    private final JdbcTemplate jdbcTemplate;
    private final ReefJournalProperties properties;

    /**
     * 创建提醒设置和订阅额度服务。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param jdbcTemplate Spring 管理的数据库访问器
     * @param properties 微信模板及提醒配置
     */
    public ReefReminderService(JdbcTemplate jdbcTemplate, ReefJournalProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    /**
     * 读取提醒偏好，首次访问时建立与原型一致的默认开关状态。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 认证上下文中的用户编号
     * @return 用户提醒时刻、开关、授权额度和已配置模板
     */
    public ReefReminderSettingsDto get(Long userId) {
        ensureDefaults(userId);
        List<String> times = jdbcTemplate.query("SELECT TIME_FORMAT(send_time, '%H:%i') FROM reef_reminder_preference WHERE user_id = ? LIMIT 1",
                (resultSet, rowNumber) -> resultSet.getString(1), userId);
        List<ReefReminderItemDto> items = new ArrayList<>();
        for (String type : reminderTypes()) {
            items.add(readItem(userId, type));
        }
        ReefReminderSettingsDto result = new ReefReminderSettingsDto();
        result.setSendTime(times.isEmpty() || times.get(ZERO) == null ? "" : times.get(ZERO));
        result.setItems(items);
        return result;
    }

    /**
     * 更新统一提醒时刻和各类提醒开关。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 认证上下文中的用户编号
     * @param settings 用户提交的提醒设置
     */
    @Transactional
    public void save(Long userId, ReefReminderSettingsDto settings) {
        if (settings.getItems() == null) throw new ReefJournalException("提醒设置列表不能为空");
        LocalTime sendTime = parseSendTime(settings.getSendTime());
        jdbcTemplate.update("INSERT INTO reef_reminder_preference (user_id, send_time, updated_at) VALUES (?, ?, NOW()) ON DUPLICATE KEY UPDATE send_time = VALUES(send_time), updated_at = NOW()",
                userId, sendTime);
        ensureDefaults(userId);
        for (ReefReminderItemDto item : settings.getItems()) {
            validateType(item.getType());
            if (item.getEnabled() == null) throw new ReefJournalException("提醒开关状态不能为空");
            jdbcTemplate.update("INSERT INTO reef_reminder_setting (user_id, type, enabled, updated_at) VALUES (?, ?, ?, NOW()) ON DUPLICATE KEY UPDATE enabled = VALUES(enabled), updated_at = NOW()",
                    userId, item.getType(), item.getEnabled() ? ONE : ZERO);
            if (item.getEnabled()) {
                jdbcTemplate.update("UPDATE reef_reminder_delivery SET status = ?, updated_at = NOW() WHERE user_id = ? AND type = ? AND status = ?",
                        STATUS_PENDING, userId, item.getType(), STATUS_CANCELLED);
            } else {
                jdbcTemplate.update("UPDATE reef_reminder_delivery SET status = ?, updated_at = NOW() WHERE user_id = ? AND type = ? AND status IN (?, ?)",
                        STATUS_CANCELLED, userId, item.getType(), STATUS_PENDING, STATUS_WAITING_CONSENT);
            }
        }
        LocalDateTime nextSendAt = sendTime == null ? null : LocalDateTime.of(java.time.LocalDate.now(ZoneId.of(TIME_ZONE)), sendTime);
        if (nextSendAt == null) {
            jdbcTemplate.update("UPDATE reef_reminder_delivery SET status = ?, updated_at = NOW() WHERE user_id = ? AND status IN (?, ?)",
                    STATUS_CANCELLED, userId, STATUS_PENDING, STATUS_WAITING_CONSENT);
        } else {
            jdbcTemplate.update("UPDATE reef_reminder_delivery SET due_at = ?, updated_at = NOW() WHERE user_id = ? AND status IN (?, ?)",
                    nextSendAt, userId, STATUS_PENDING, STATUS_WAITING_CONSENT);
        }
    }

    /**
     * 记录小程序用户在微信订阅弹窗中的单次授权结果。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 认证上下文中的用户编号
     * @param request 用户授权结果
     */
    @Transactional
    public void recordConsent(Long userId, ReefSubscriptionConsentRequest request) {
        validateType(request.getType());
        if (!request.getAccepted()) return;
        jdbcTemplate.update("INSERT INTO reef_subscription_quota (user_id, type, available_count, updated_at) VALUES (?, ?, ?, NOW()) ON DUPLICATE KEY UPDATE available_count = available_count + VALUES(available_count), updated_at = NOW()",
                userId, request.getType(), ONE);
        jdbcTemplate.update("UPDATE reef_reminder_delivery SET status = ?, updated_at = NOW() WHERE user_id = ? AND type = ? AND status = ?",
                STATUS_PENDING, userId, request.getType(), STATUS_WAITING_CONSENT);
    }

    /**
     * 返回提醒类型的预设间隔天数，设备维护周期由设备记录维护。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param type 提醒类型
     * @return 固定周期提醒间隔；设备提醒返回空值
     */
    public Integer intervalDays(String type) {
        if (TYPE_WATER_CHANGE.equals(type)) return WATER_CHANGE_INTERVAL_DAYS;
        if (TYPE_WATER_TEST.equals(type)) return WATER_TEST_INTERVAL_DAYS;
        if (TYPE_FEEDING.equals(type)) return FEEDING_INTERVAL_DAYS;
        return null;
    }

    /**
     * 确认提醒发送前用户仍保持该类提醒开启。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 用户编号
     * @param type 提醒类型
     * @return 该类提醒当前是否启用
     */
    public boolean isEnabled(Long userId, String type) {
        List<Integer> enabled = jdbcTemplate.query("SELECT enabled FROM reef_reminder_setting WHERE user_id = ? AND type = ? LIMIT 1",
                (resultSet, rowNumber) -> resultSet.getInt(1), userId, type);
        return !enabled.isEmpty() && enabled.get(ZERO) == ONE;
    }

    /**
     * 首次访问时按原型建立提醒开关初始状态。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 当前用户编号
     */
    private void ensureDefaults(Long userId) {
        jdbcTemplate.update("INSERT INTO reef_reminder_preference (user_id, send_time, updated_at) VALUES (?, NULL, NOW()) ON DUPLICATE KEY UPDATE user_id = VALUES(user_id)", userId);
        for (String type : reminderTypes()) {
            jdbcTemplate.update("INSERT INTO reef_reminder_setting (user_id, type, enabled, updated_at) VALUES (?, ?, ?, NOW()) ON DUPLICATE KEY UPDATE user_id = VALUES(user_id)",
                    userId, type, defaultEnabled(type));
        }
    }

    /**
     * 读取单项提醒状态、授权额度和已配置模板编号。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 当前用户编号
     * @param type 提醒类型
     * @return 单项提醒配置
     */
    private ReefReminderItemDto readItem(Long userId, String type) {
        List<Integer> enabledRows = jdbcTemplate.query("SELECT enabled FROM reef_reminder_setting WHERE user_id = ? AND type = ? LIMIT 1",
                (resultSet, rowNumber) -> resultSet.getInt(1), userId, type);
        List<Integer> quotas = jdbcTemplate.query("SELECT available_count FROM reef_subscription_quota WHERE user_id = ? AND type = ? LIMIT 1",
                (resultSet, rowNumber) -> resultSet.getInt(1), userId, type);
        ReefReminderItemDto item = new ReefReminderItemDto();
        item.setType(type);
        item.setEnabled(!enabledRows.isEmpty() && enabledRows.get(ZERO) == ONE);
        item.setAvailableCount(quotas.isEmpty() ? ZERO : quotas.get(ZERO));
        item.setTemplateId(templateId(type));
        return item;
    }

    /**
     * 根据原型初始开关状态返回新用户默认值。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param type 提醒类型
     * @return 是否启用的整数值
     */
    private int defaultEnabled(String type) {
        if (TYPE_WATER_CHANGE.equals(type)) return DEFAULT_WATER_CHANGE_ENABLED;
        if (TYPE_WATER_TEST.equals(type)) return DEFAULT_WATER_TEST_ENABLED;
        if (TYPE_FEEDING.equals(type)) return DEFAULT_FEEDING_ENABLED;
        return DEFAULT_EQUIPMENT_ENABLED;
    }

    /**
     * 返回模块支持的提醒类型固定清单。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @return 提醒类型编码集合
     */
    private List<String> reminderTypes() {
        return Arrays.asList(TYPE_WATER_CHANGE, TYPE_WATER_TEST, TYPE_FEEDING, TYPE_EQUIPMENT);
    }

    /**
     * 根据提醒类型读取服务端配置的微信模板编号。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param type 提醒类型
     * @return 模板编号；配置缺失时返回空值
     */
    private String templateId(String type) {
        if (TYPE_WATER_CHANGE.equals(type)) return properties.getWaterChangeTemplateId();
        if (TYPE_WATER_TEST.equals(type)) return properties.getWaterTestTemplateId();
        if (TYPE_FEEDING.equals(type)) return properties.getFeedingTemplateId();
        return properties.getEquipmentTemplateId();
    }

    /**
     * 解析用户选择的统一上海提醒时刻。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param sendTime 小时分钟文本
     * @return 本地提醒时刻；未选择时返回空值
     */
    private LocalTime parseSendTime(String sendTime) {
        if (sendTime == null || sendTime.trim().isEmpty()) return null;
        try {
            return LocalTime.parse(sendTime.trim());
        } catch (RuntimeException exception) {
            throw new ReefJournalException("提醒时刻请使用小时和分钟格式");
        }
    }

    /**
     * 校验客户端提交的提醒类型属于固定支持范围。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param type 提醒类型编码
     */
    private void validateType(String type) {
        if (!reminderTypes().contains(type)) throw new ReefJournalException("提醒类型不受支持");
    }
}
