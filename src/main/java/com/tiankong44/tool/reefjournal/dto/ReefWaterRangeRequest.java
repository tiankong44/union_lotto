package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * 鱼缸水质范围批量保存请求。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
public class ReefWaterRangeRequest {
    @Valid
    @NotNull(message = "水质范围列表不能为空")
    private List<ReefWaterRangeItemRequest> ranges = new ArrayList<>();
}
