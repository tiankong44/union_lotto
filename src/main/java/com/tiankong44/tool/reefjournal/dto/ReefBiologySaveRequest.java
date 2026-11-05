package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 生物新增或更新请求。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
public class ReefBiologySaveRequest {
    private String id;
    @NotBlank(message = "生物名称不能为空")
    @Size(max = 128, message = "生物名称不能超过128个字符")
    private String name;
    @Size(max = 128, message = "学名不能超过128个字符")
    private String scientificName;
    @NotBlank(message = "生物分类不能为空")
    private String category;
    @NotBlank(message = "入缸日期不能为空")
    private String addedAt;
    private BigDecimal bodyLengthCm;
    @Size(max = 128, message = "来源不能超过128个字符")
    private String source;
    private String status;
    @NotNull(message = "鱼缸编号不能为空")
    private String tankId;
}
