package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 鱼缸详情和首页卡片响应。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
public class ReefTankDto {
    private String id;
    private String name;
    private String startedAt;
    private String capacityLiter;
    private String systemType;
    /** 水体类型和饲养主题均由用户维护，旧资料可为空。 */
    private String waterType;
    private String themes;
    private String enabledMetrics;
    private Boolean archived;
    private String note;
    private Integer daysOpen;
    private Integer biologyCount;
    private Integer equipmentCount;
    private String lastRecordSummary;
    private List<ReefWaterRangeDto> waterRanges = new ArrayList<>();
}
