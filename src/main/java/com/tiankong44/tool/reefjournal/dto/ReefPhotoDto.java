package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

/**
 * 照片墙记录响应，文件内容通过受保护接口读取。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
public class ReefPhotoDto {
    private String id;
    private String title;
    private String category;
    private String referenceType;
    private String referenceId;
    private String recordedAt;
    private String note;
    private String contentUrl;
    private String motionContentUrl;
}
