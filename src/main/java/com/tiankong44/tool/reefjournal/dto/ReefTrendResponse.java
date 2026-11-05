package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 水质趋势与用户自设范围提醒响应。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
public class ReefTrendResponse {
    private ReefWaterStatsDto stats;
    private List<ReefWaterPointDto> points = new ArrayList<>();
    private List<ReefAlertDto> alerts = new ArrayList<>();
}
