package com.tiankong44.tool.reefjournal;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 水族档案、测量项目和维护日志的固定编码及计量单位。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
public final class ReefAquariumConstants {
    public static final List<String> WATER_TYPES = Collections.unmodifiableList(Arrays.asList("FRESH", "MARINE", "BRACKISH", "OTHER"));
    public static final List<String> TANK_THEMES = Collections.unmodifiableList(Arrays.asList("FISH", "PLANTED", "CORAL", "SHRIMP", "QUARANTINE", "BREEDING", "OTHER"));
    public static final List<String> BIOLOGY_CATEGORIES = Collections.unmodifiableList(Arrays.asList("FISH", "CORAL", "PLANT", "SHRIMP", "SNAIL", "CRAB", "OTHER"));
    public static final List<String> BIOLOGY_EVENTS = Collections.unmodifiableList(Arrays.asList("OBSERVATION", "STATUS", "TRANSFER", "GROWTH", "BREEDING", "PRUNING", "LOSS", "OTHER"));
    public static final List<String> ACTIVITY_TYPES = Collections.unmodifiableList(Arrays.asList("FERTILIZING", "SUPPLEMENT", "PRUNING", "TOP_OFF", "MAINTENANCE", "LIGHTING", "SALT_MIX"));
    public static final List<String> EQUIPMENT_CATEGORIES = Collections.unmodifiableList(Arrays.asList("LIGHT", "FILTER", "TEMPERATURE", "CO2", "PUMP", "SKIMMER", "DOSING", "AUTO_TOP_OFF", "OTHER"));
    public static final String TRACKING_SINGLE = "SINGLE";
    public static final String TRACKING_BATCH = "BATCH";
    public static final String EVENT_TRANSFER = "TRANSFER";
    public static final String PHOTO_REFERENCE_EVENT = "BIOLOGY_EVENT";
    public static final String BACKUP_TANK = "TANK";
    public static final String BACKUP_BIOLOGY = "BIOLOGY";
    public static final String BACKUP_LOG = "LOG";
    public static final String BACKUP_EQUIPMENT = "EQUIPMENT";
    public static final String BACKUP_EVENT = "BIOLOGY_EVENT";
    public static final String BACKUP_PHOTO = "PHOTO";
    public static final String BACKUP_RANGE = "WATER_RANGE";
    public static final int BACKUP_BATCH_LIMIT = 100;
    public static final Map<String, String> WATER_METRIC_UNITS;

    static {
        // 每个编码对应固定计量对象及单位，避免把不同物理量合并。
        Map<String, String> units = new LinkedHashMap<>();
        units.put("salinity", "比重");
        units.put("salinity_ppt", "‰");
        units.put("temperature", "℃");
        units.put("ph", "");
        units.put("no2", "mg/L");
        units.put("no3", "mg/L");
        units.put("po4", "mg/L");
        units.put("total_ammonia_n", "mg/L N");
        units.put("free_ammonia", "mg/L NH₃");
        units.put("gh", "°dGH");
        units.put("kh", "°dKH");
        units.put("oxygen", "mg/L");
        units.put("tds", "mg/L");
        units.put("co2", "mg/L");
        units.put("iron", "mg/L");
        units.put("potassium", "mg/L");
        units.put("calcium", "mg/L");
        units.put("magnesium", "mg/L");
        WATER_METRIC_UNITS = Collections.unmodifiableMap(units);
    }

    /** 禁止实例化业务常量。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    private ReefAquariumConstants() { }
}
