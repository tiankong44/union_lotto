package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 影像记录详情修改请求。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
public class ReefPhotoSaveRequest {
    @NotBlank(message = "照片分类不能为空")
    private String category;
    @Size(max = 255, message = "照片名称不能超过255个字符")
    private String title;
    @Size(max = 32, message = "关联类型不能超过32个字符")
    private String referenceType;
    @Size(max = 32, message = "关联编号不能超过32个字符")
    private String referenceId;
    @NotBlank(message = "照片时间不能为空")
    private String recordedAt;
    @Size(max = 2000, message = "备注不能超过2000个字符")
    private String note;
}
