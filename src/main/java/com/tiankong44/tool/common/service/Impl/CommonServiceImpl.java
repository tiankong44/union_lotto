package com.tiankong44.tool.common.service.Impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tiankong44.tool.base.entity.BaseRes;
import com.tiankong44.tool.common.dto.CloudFileUploadResponse;
import com.tiankong44.tool.common.entity.CloudFile;
import com.tiankong44.tool.common.mapper.CloudFileMapper;
import com.tiankong44.tool.common.service.CommonService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 公共文件服务，负责将新上传图片保存到云端 MySQL。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-08-06
 */
@Service
public class CommonServiceImpl implements CommonService {
    // 图片内容上限与 multipart 配置保持一致，避免大文件占用过多服务内存。
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024L;

    @Resource
    private CloudFileMapper cloudFileMapper;

    /**
     * 上传图片并保存二进制内容及元数据。
     *
     * @param uploadFile 图片文件，必须非空、类型为 image/* 且不超过10MB
     * @return 文件访问信息；参数非法或读取、写入失败时返回失败响应
     */
    @Override
    public BaseRes<CloudFileUploadResponse> uploadIcon(MultipartFile uploadFile) {
        if (uploadFile == null || uploadFile.isEmpty()) {
            return BaseRes.failure("上传图片不能为空");
        }
        if (uploadFile.getSize() > MAX_IMAGE_SIZE) {
            return BaseRes.failure("图片大小不能超过10MB");
        }
        String contentType = uploadFile.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            return BaseRes.failure("只允许上传图片文件");
        }

        try {
            // 先在内存中读取上传内容，避免业务文件写入服务器本地目录。
            byte[] content = uploadFile.getBytes();
            String fileKey = UUID.randomUUID().toString().replace("-", "");
            CloudFile cloudFile = new CloudFile();
            cloudFile.setFileKey(fileKey);
            cloudFile.setOriginalName(uploadFile.getOriginalFilename());
            cloudFile.setContentType(contentType);
            cloudFile.setFileSize((long) content.length);
            cloudFile.setFileMd5(DigestUtil.md5Hex(content));
            cloudFile.setFileContent(content);
            cloudFile.setCreatedAt(LocalDateTime.now());
            // 调用云端文件 Mapper，将元数据和二进制内容一次写入 MySQL。
            cloudFileMapper.insert(cloudFile);

            CloudFileUploadResponse response = new CloudFileUploadResponse();
            response.setFileKey(fileKey);
            response.setOriginalName(cloudFile.getOriginalName());
            response.setContentType(contentType);
            response.setFileSize(cloudFile.getFileSize());
            response.setFileMd5(cloudFile.getFileMd5());
            response.setDownloadUrl("/tabs/common/files/" + fileKey);
            return BaseRes.success(response);
        } catch (IOException exception) {
            return BaseRes.failure("读取上传图片失败");
        } catch (RuntimeException exception) {
            return BaseRes.failure("保存上传图片失败");
        }
    }

    /**
     * 根据文件编号查询图片内容。
     *
     * @param fileKey 文件编号，空值直接视为不存在
     * @return 文件实体，不存在时返回空值
     */
    @Override
    public CloudFile getFile(String fileKey) {
        if (fileKey == null || fileKey.trim().isEmpty()) {
            return null;
        }
        // 按随机文件编号查询，避免使用原始文件名构造本地路径。
        return cloudFileMapper.selectOne(new QueryWrapper<CloudFile>().eq("file_key", fileKey));
    }
}
