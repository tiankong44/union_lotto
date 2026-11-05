package com.tiankong44.tool.reefjournal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 首页鱼缸摘要响应。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReefDashboardResponse {
    private List<ReefTankDto> tanks = new ArrayList<>();
}
