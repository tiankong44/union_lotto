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
    private String salinity;
    private String temperature;
    private String ph;
    private String no2;
    private String no3;
    private String po4;
    private List<String> biologyIds = new ArrayList<>();
}
