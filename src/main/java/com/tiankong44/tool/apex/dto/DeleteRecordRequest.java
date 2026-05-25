package com.tiankong44.tool.apex.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class DeleteRecordRequest {
    @NotNull(message = "记录不能为空")
    @Min(value = 1, message = "数量必须大于0")
    private Integer id;
}