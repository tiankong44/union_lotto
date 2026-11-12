package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 逐张恢复照片时使用的备份内编号和影像元数据。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
@Data
public class ReefBackupPhotoRequest {
    @NotBlank(message = "备份照片编号不能为空")
    @Size(max = 32, message = "备份照片编号过长")
    private String originalId;
    @NotBlank(message = "照片分类不能为空")
    private String category;
    @Size(max = 255, message = "照片标题不能超过255个字符")
    private String title;
    private String referenceType;
    @Size(max = 32, message = "关联编号过长")
    private String referenceId;
    @NotBlank(message = "照片时间不能为空")
    private String recordedAt;
    @Size(max = 2000, message = "照片备注不能超过2000个字符")
    private String note;
    @NotBlank(message = "照片类型不能为空")
    @Size(max = 128, message = "照片类型过长")
    private String contentType;
}
