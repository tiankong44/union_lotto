package com.tiankong44.tool.common.dto;

import lombok.Data;

/**
 * 云端文件上传结果，向调用方返回文件元数据和下载地址。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-08-06
 */
@Data
public class CloudFileUploadResponse {
    private String fileKey;
    private String originalName;
    private String contentType;
    private Long fileSize;
    private String fileMd5;
    private String downloadUrl;
}
