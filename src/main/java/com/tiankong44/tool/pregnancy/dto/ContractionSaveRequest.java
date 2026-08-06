package com.tiankong44.tool.pregnancy.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 宫缩记录保存请求。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-08-06
 */
@Data
public class ContractionSaveRequest {
    @NotBlank(message = "客户端记录编号不能为空")
    @Size(max = 64, message = "客户端记录编号不能超过64个字符")
    private String clientRecordId;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startedAt;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endedAt;

    @NotNull(message = "持续时间不能为空")
    @Min(value = 0, message = "持续时间不能小于0")
    private Integer durationSeconds;

    @Min(value = 0, message = "间隔时间不能小于0")
    private Integer intervalSeconds;

    @Min(value = 1, message = "强度不能小于1")
    @Max(value = 5, message = "强度不能超过5")
    private Integer intensity;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String note;
}
