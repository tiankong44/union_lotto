package com.tiankong44.tool.reefjournal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 水质趋势图中的单个采样点。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReefWaterPointDto {
    private String recordedAt;
    private String value;
}
