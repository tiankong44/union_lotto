package com.tiankong44.tool.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 云端文件实体，保存图片元数据和 MySQL 二进制内容。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-08-06
 */
@Data
@TableName("cloud_file")
public class CloudFile {
    // 数据库自增主键，仅用于云端文件内部定位。
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 对外暴露的随机文件编号，不使用原始文件名作为访问标识。
    @TableField("file_key")
    private String fileKey;

    // 上传时的原始文件名，仅用于下载响应展示。
    @TableField("original_name")
    private String originalName;

    // 文件媒体类型，用于下载响应的 Content-Type。
    @TableField("content_type")
    private String contentType;

    // 文件字节数，用于响应长度和容量统计。
    @TableField("file_size")
    private Long fileSize;

    // 文件内容摘要，用于文件完整性识别。
    @TableField("file_md5")
    private String fileMd5;

    // 文件二进制内容，直接保存到 MySQL MEDIUMBLOB。
    @TableField("file_content")
    private byte[] fileContent;

    // 文件首次写入云端的时间。
    @TableField("created_at")
    private LocalDateTime createdAt;
}
