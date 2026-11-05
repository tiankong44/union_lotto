package com.tiankong44.tool.reefjournal;

/**
 * 海缸日记模块集中维护的业务常量。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
public final class ReefJournalConstants {
    public static final String REQUEST_USER_ID = "reefUserId";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String TOKEN_SEPARATOR = ".";
    public static final String TOKEN_PAYLOAD_SEPARATOR = ":";
    public static final String PHOTO_TOKEN_SEPARATOR = ".";
    public static final String TIME_ZONE = "Asia/Shanghai";
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String TIME_PATTERN = "HH:mm";
    public static final String TYPE_WATER_CHANGE = "WATER_CHANGE";
    public static final String TYPE_WATER_TEST = "WATER_TEST";
    public static final String TYPE_FEEDING = "FEEDING";
    public static final String TYPE_EQUIPMENT = "EQUIPMENT";
    public static final String TYPE_MEDICATION = "MEDICATION";
    public static final String TYPE_OBSERVATION = "OBSERVATION";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SENDING = "SENDING";
    public static final String STATUS_WAITING_CONSENT = "WAITING_CONSENT";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String REDIS_REMINDER_LOCK = "reef-journal:reminder-dispatch-lock";
    public static final String REDIS_LOCK_RELEASE_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
    public static final String LABEL_WATER_CHANGE = "换水提醒";
    public static final String LABEL_WATER_TEST = "水质检测提醒";
    public static final String LABEL_FEEDING = "喂食提醒";
    public static final String LABEL_EQUIPMENT = "设备维护提醒";
    public static final String METRIC_SALINITY = "salinity";
    public static final String METRIC_TEMPERATURE = "temperature";
    public static final String METRIC_PH = "ph";
    public static final String METRIC_NO2 = "no2";
    public static final String METRIC_NO3 = "no3";
    public static final String METRIC_PO4 = "po4";
    public static final String BIOLOGY_STATUS_NORMAL = "NORMAL";
    public static final String BIOLOGY_STATUS_QUARANTINE = "QUARANTINE";
    public static final String BIOLOGY_STATUS_ISOLATED = "ISOLATED";
    public static final String BIOLOGY_STATUS_SICK = "SICK";
    public static final String BIOLOGY_STATUS_DEAD = "DEAD";
    public static final String CATEGORY_FISH = "FISH";
    public static final String CATEGORY_CORAL = "CORAL";
    public static final String CATEGORY_PLANT = "PLANT";
    public static final String CATEGORY_OTHER = "OTHER";
    public static final String EQUIPMENT_STATUS_RUNNING = "RUNNING";
    public static final String EQUIPMENT_STATUS_DIRTY = "DIRTY";
    public static final String EQUIPMENT_STATUS_STANDBY = "STANDBY";
    public static final String PHOTO_CATEGORY_TANK = "TANK";
    public static final String PHOTO_CATEGORY_FISH = "FISH";
    public static final String PHOTO_CATEGORY_CORAL = "CORAL";
    public static final String PHOTO_CATEGORY_EQUIPMENT = "EQUIPMENT";
    public static final String PHOTO_CATEGORY_PLANT = "PLANT";
    public static final int DEFAULT_PAGE_NUMBER = 1;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int WATER_CHANGE_INTERVAL_DAYS = 14;
    public static final int WATER_TEST_INTERVAL_DAYS = 7;
    public static final int FEEDING_INTERVAL_DAYS = 1;
    public static final int DEFAULT_WATER_CHANGE_ENABLED = 1;
    public static final int DEFAULT_WATER_TEST_ENABLED = 1;
    public static final int DEFAULT_FEEDING_ENABLED = 0;
    public static final int DEFAULT_EQUIPMENT_ENABLED = 1;
    public static final int ZERO = 0;
    public static final int ONE = 1;
    public static final int HTTP_UNAUTHORIZED = 401;
    public static final int HTTP_SERVICE_UNAVAILABLE = 503;
    public static final long MAX_PHOTO_BYTES = 10L * 1024L * 1024L;

    /**
     * 禁止实例化静态常量集合。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     */
    private ReefJournalConstants() {
    }
}
