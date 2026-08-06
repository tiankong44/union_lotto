package com.tiankong44.tool.pregnancy.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 单条本地同步记录请求。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-08-06
 */
@Data
public class SyncBatchItem {
    @NotBlank(message = "实体类型不能为空")
    @Size(max = 32, message = "实体类型不能超过32个字符")
    private String entityType;

    @NotBlank(message = "客户端记录编号不能为空")
    @Size(max = 64, message = "客户端记录编号不能超过64个字符")
    private String clientRecordId;

    @NotBlank(message = "同步内容不能为空")
    @Size(max = 10000, message = "同步内容不能超过10000个字符")
    private String payload;
}
