package com.tiankong44.tool.reefjournal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 海缸列表分页响应，记录总数使用字符串避免大整数精度损失。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReefPageResponse<T> {
    private Integer pageNum;
    private Integer pageSize;
    private String total;
    private List<T> list;
}
