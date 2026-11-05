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
    private Integer daysOpen;
    private Integer biologyCount;
    private Integer equipmentCount;
    private String lastRecordSummary;
    private List<ReefWaterRangeDto> waterRanges = new ArrayList<>();
}
