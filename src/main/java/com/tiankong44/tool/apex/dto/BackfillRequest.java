package com.tiankong44.tool.apex.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class BackfillRequest {
    @NotNull(message = "日期不能为空")
    private LocalDate date;
    
    @NotNull(message = "数量不能为空")
    @Min(value = 0, message = "数量不能小于0")
    private Integer count;
}