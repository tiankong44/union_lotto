package com.tiankong44.tool.reefjournal.support;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static com.tiankong44.tool.reefjournal.ReefJournalConstants.DATE_TIME_PATTERN;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.DATE_PATTERN;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.TIME_ZONE;

/**
 * 处理海缸模块统一使用的上海本地日期时间格式。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
public final class ReefDateTimeSupport {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    /**
     * 禁止实例化静态日期时间支持类。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     */
    private ReefDateTimeSupport() {
    }

    /**
     * 解析小程序提交的日期文本。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param value yyyy-MM-dd 日期文本
     * @return 本地日期
     */
    public static LocalDate parseDate(String value) {
        return LocalDate.parse(value.trim(), DATE_FORMATTER);
    }

    /**
     * 解析带空格或 T 分隔符的上海本地时间文本。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param value yyyy-MM-dd HH:mm:ss 时间文本
     * @return 本地日期时间
     */
    public static LocalDateTime parseDateTime(String value) {
        String normalized = value.trim().replace('T', ' ');
        if (normalized.length() == 10) normalized = normalized + " 00:00:00";
        if (normalized.length() == 16) normalized = normalized + ":00";
        return LocalDateTime.parse(normalized, DATE_TIME_FORMATTER);
    }

    /**
     * 按统一格式序列化本地日期时间。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param value 本地日期时间
     * @return yyyy-MM-dd HH:mm:ss 文本
     */
    public static String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_FORMATTER);
    }

    /**
     * 取得项目上海时区的当前日期时间。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @return 上海本地当前时间
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of(TIME_ZONE));
    }
}
