package com.tiankong44.tool.common.controller;

import com.tiankong44.tool.base.entity.BaseRes;
import com.tiankong44.tool.common.dto.CloudFileUploadResponse;
import com.tiankong44.tool.common.entity.CloudFile;
import com.tiankong44.tool.common.service.CommonService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;

/**
 * 公共云端文件接口，提供图片上传和读取能力。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-08-06
 */
@RestController
@RequestMapping("/common")
public class CommonController {
    @Resource
    private CommonService commonService;

    /**
     * 上传图片到云端 MySQL。
     *
     * @param file 图片文件
     * @return 上传结果或业务失败信息
     */
    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseRes<CloudFileUploadResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        // 调用文件服务完成类型校验、内容读取和云端落库。
        return commonService.uploadIcon(file);
    }

    /**
     * 根据随机文件编号读取云端图片。
     *
     * @param fileKey 文件编号
     * @return 图片二进制内容；文件不存在时返回404
     */
    @GetMapping("/files/{fileKey}")
    public ResponseEntity<byte[]> getFile(@PathVariable String fileKey) {
        // 调用文件服务读取云端内容，不访问服务器本地目录。
        CloudFile cloudFile = commonService.getFile(fileKey);
        if (cloudFile == null) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (cloudFile.getContentType() != null) {
            try {
                mediaType = MediaType.parseMediaType(cloudFile.getContentType());
            } catch (IllegalArgumentException ignored) {
                // 数据库中的异常类型使用二进制默认类型返回，避免下载接口直接失败。
            }
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentLength(cloudFile.getFileSize() == null ? 0L : cloudFile.getFileSize());
        headers.setContentDisposition(ContentDisposition.inline()
                .filename(cloudFile.getOriginalName() == null ? cloudFile.getFileKey() : cloudFile.getOriginalName(), StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok().headers(headers).body(cloudFile.getFileContent());
    }
}
