package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 海缸日志新增或更新请求。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
public class ReefLogRequest {
    private String id;
    @NotNull(message = "鱼缸编号不能为空")
    private String tankId;
    @NotBlank(message = "日志类型不能为空")
    private String type;
    @NotBlank(message = "日志标题不能为空")
    @Size(max = 128, message = "日志标题不能超过128个字符")
    private String title;
    @Size(max = 1000, message = "日志摘要不能超过1000个字符")
    private String description;
    @NotBlank(message = "记录时间不能为空")
    private String recordedAt;
    @Size(max = 2000, message = "备注不能超过2000个字符")
    private String note;
    private BigDecimal changePercent;
    @Valid
    private ReefWaterValues water;
    private List<String> biologyIds = new ArrayList<>();
}
