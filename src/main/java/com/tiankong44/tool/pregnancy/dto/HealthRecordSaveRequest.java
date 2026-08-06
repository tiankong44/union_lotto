package com.tiankong44.tool.pregnancy.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 孕期健康记录保存请求。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-08-06
 */
@Data
public class HealthRecordSaveRequest {
    @NotBlank(message = "记录类型不能为空")
    @Size(max = 32, message = "记录类型不能超过32个字符")
    private String recordType;

    @NotBlank(message = "记录值不能为空")
    @Size(max = 2000, message = "记录值不能超过2000个字符")
    private String valueJson;

    @Size(max = 16, message = "单位不能超过16个字符")
    private String unit;

    @NotNull(message = "记录时间不能为空")
    private LocalDateTime recordedAt;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String note;

    @NotBlank(message = "客户端记录编号不能为空")
    @Size(max = 64, message = "客户端记录编号不能超过64个字符")
    private String clientRecordId;
}
