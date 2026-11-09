package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 多项水质历史趋势总览及当前选中参数的完整采样点。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-09
 */
@Data
public class ReefWaterDashboardDto {
    /** 当前详细趋势对应的水质参数编码。 */
    private String metric;
    /** 六项水质参数的统计摘要与抽样点；无数据时返回空数组。 */
    private List<ReefWaterSeriesDto> series = new ArrayList<>();
    /** 当前参数在所选范围内的完整历史采样点；无数据时返回空数组。 */
    private List<ReefWaterPointDto> points = new ArrayList<>();
    /** 按鱼缸自设范围生成的提醒；无数据时返回空数组。 */
    private List<ReefAlertDto> alerts = new ArrayList<>();
}
