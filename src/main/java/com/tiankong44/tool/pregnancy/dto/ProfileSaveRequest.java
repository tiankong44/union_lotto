package com.tiankong44.tool.pregnancy.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 孕期档案保存请求。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-08-06
 */
@Data
public class ProfileSaveRequest {
    private LocalDate lmpDate;
    private LocalDate dueDate;

    @Min(value = 1, message = "胎儿数量必须大于0")
    @Max(value = 4, message = "胎儿数量不能超过4")
    private Integer babyCount = 1;

    @Size(max = 64, message = "称呼不能超过64个字符")
    private String nickname;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String note;
}
