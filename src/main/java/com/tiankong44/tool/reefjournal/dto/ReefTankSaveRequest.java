package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 鱼缸新增或更新请求。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
public class ReefTankSaveRequest {
    private String id;
    @NotBlank(message = "鱼缸名称不能为空")
    @Size(max = 128, message = "鱼缸名称不能超过128个字符")
    private String name;
    @NotBlank(message = "开缸日期不能为空")
    private String startedAt;
    private BigDecimal capacityLiter;
    @Size(max = 64, message = "系统类型不能超过64个字符")
    private String systemType;
    @Size(max = 1000, message = "备注不能超过1000个字符")
    private String note;
}
