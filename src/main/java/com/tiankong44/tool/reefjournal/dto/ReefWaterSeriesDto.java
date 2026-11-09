package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 单项水质参数的历史统计和小图采样数据。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-09
 */
@Data
public class ReefWaterSeriesDto {
    /** 水质参数编码。 */
    private String metric;
    /** 最新值、近七日均值及最近两次记录的变化方向。 */
    private ReefWaterStatsDto stats;
    /** 所选时间范围内该参数的有效检测次数。 */
    private int sampleCount;
    /** 用于总览小图的均匀抽样点，避免返回完整历史序列。 */
    private List<ReefWaterPointDto> samplePoints = new ArrayList<>();
}
