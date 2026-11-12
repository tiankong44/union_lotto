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
    /** 仅供恢复备份时保留进入当前鱼缸的日期。 */
    private String currentTankSince;
    private BigDecimal bodyLengthCm;
    @Size(max = 128, message = "来源不能超过128个字符")
    private String source;
    private String status;
    /** 记录方式、数量和缸内位置由用户填写。 */
    private String trackingMode;
    private Integer quantity;
    @Size(max = 255, message = "缸内位置不能超过255个字符")
    private String positionNote;
    @Size(max = 1000, message = "补充说明不能超过1000个字符")
    private String note;
    @NotNull(message = "鱼缸编号不能为空")
    private String tankId;
}
