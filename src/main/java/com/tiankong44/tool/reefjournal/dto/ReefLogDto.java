package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 日志列表和水质趋势采样响应。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
public class ReefLogDto {
    private String id;
    private String tankId;
    private String type;
    private String title;
    private String description;
    private String recordedAt;
    private String note;
    private String tankName;
    private String changePercent;
    /** 日常维护明细和逐项水质测量。 */
    private String activityItem;
    private String activityAmount;
    private String activityUnit;
    private String waterSource;
    private String waterVolumeLiter;
    private String lightHours;
    private List<ReefWaterMeasurementDto> measurements = new ArrayList<>();
    private String salinity;
    private String temperature;
    private String ph;
    private String no2;
    private String no3;
    private String po4;
    private List<String> biologyIds = new ArrayList<>();
    /** 保存时关联的生物摘要，转缸后仍可在历史日志中查看。 */
    private List<ReefBiologyDto> biology = new ArrayList<>();
}
