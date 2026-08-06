package com.tiankong44.tool.pregnancy.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 本地同步批次请求。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-08-06
 */
@Data
public class SyncBatchRequest {
    @NotEmpty(message = "同步列表不能为空")
    private List<@Valid SyncBatchItem> items;
}
