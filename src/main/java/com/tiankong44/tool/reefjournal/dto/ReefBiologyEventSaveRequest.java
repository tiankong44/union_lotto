package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 生物事件新增请求。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
public class ReefBiologyEventSaveRequest {
    @NotBlank(message = "事件标题不能为空")
    @Size(max = 128, message = "事件标题不能超过128个字符")
    private String title;
    @NotBlank(message = "事件时间不能为空")
    private String eventAt;
    @Size(max = 1000, message = "事件描述不能超过1000个字符")
    private String description;
    private String status;
    /** 事件类型、目标鱼缸和事件数量均为用户记录。 */
    private String eventType;
    private String toTankId;
    private Integer quantity;
}
