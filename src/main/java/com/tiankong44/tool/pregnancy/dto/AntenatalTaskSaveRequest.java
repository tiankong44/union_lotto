package com.tiankong44.tool.pregnancy.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 产检待办保存请求。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-08-06
 */
@Data
public class AntenatalTaskSaveRequest {
    @NotBlank(message = "客户端记录编号不能为空")
    @Size(max = 64, message = "客户端记录编号不能超过64个字符")
    private String clientRecordId;

    @NotBlank(message = "待办标题不能为空")
    @Size(max = 128, message = "待办标题不能超过128个字符")
    private String title;

    @Size(max = 32, message = "待办类型不能超过32个字符")
    private String taskType;

    @NotNull(message = "计划时间不能为空")
    private LocalDateTime plannedAt;

    @Size(max = 16, message = "状态不能超过16个字符")
    private String status;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String note;
}
