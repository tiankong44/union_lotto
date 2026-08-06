package com.tiankong44.tool.pregnancy.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 孕期历史记录删除请求。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-08-06
 */
@Data
public class RecordDeleteRequest {
    @NotBlank(message = "记录类型不能为空")
    @Pattern(regexp = "movement|contraction|health|task", message = "记录类型不支持删除")
    private String recordType;

    @NotBlank(message = "客户端记录编号不能为空")
    @Size(max = 64, message = "客户端记录编号不能超过64个字符")
    private String clientRecordId;
}
