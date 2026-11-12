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
import java.util.Locale;
import java.util.UUID;

import static com.tiankong44.tool.common.CloudFileConstants.MAX_FILE_BYTES;
import static com.tiankong44.tool.common.CloudFileConstants.VIDEO_CONTENT_TYPES;

/**
 * 公共文件服务，负责将新上传图片和视频保存到云端 MySQL。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-08-06
 */
@Service
public class CommonServiceImpl implements CommonService {
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
        if (uploadFile.getSize() > MAX_FILE_BYTES) {
            return BaseRes.failure("图片大小不能超过10MB");
        }
        String contentType = uploadFile.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return BaseRes.failure("只允许上传图片文件");
        }
        // 将头像图片交给公共方法写入共享云端文件表。
        return storeFile(uploadFile, contentType, "图片");
    }

    /**
     * 校验实况照片动态片段格式后保存到共享云端文件表。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-12
     * @param uploadFile 实况照片对应的视频片段
     * @return 文件访问信息，参数非法或保存失败时返回失败响应
     */
    @Override
    public BaseRes<CloudFileUploadResponse> uploadVideo(MultipartFile uploadFile) {
        if (uploadFile == null || uploadFile.isEmpty()) return BaseRes.failure("先选择实况照片的动态片段");
        if (uploadFile.getSize() > MAX_FILE_BYTES) return BaseRes.failure("动态片段大小不能超过10MB");
        String contentType = uploadFile.getContentType();
        String normalizedType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (!VIDEO_CONTENT_TYPES.contains(normalizedType)) return BaseRes.failure("动态片段仅支持 MP4、MOV、M4V 或 3GP 格式");
        // 将通过类型白名单校验的视频片段交给公共方法写入共享云端文件表。
        return storeFile(uploadFile, normalizedType, "动态片段");
    }

    /**
     * 将经过类型校验的文件写入共享云端文件表。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-12
     * @param uploadFile 已校验的 multipart 文件
     * @param contentType 已校验的媒体类型
     * @param label 用于返回具体错误信息的媒体名称
     * @return 文件访问信息或明确的保存错误
     */
    private BaseRes<CloudFileUploadResponse> storeFile(MultipartFile uploadFile, String contentType, String label) {
        try {
            // 先在内存中读取上传内容，避免业务文件写入服务器本地目录。
            byte[] content = uploadFile.getBytes();
            String fileKey = UUID.randomUUID().toString().replace("-", "");
            CloudFile cloudFile = new CloudFile();
            cloudFile.setFileKey(fileKey);
            String originalName = uploadFile.getOriginalFilename();
            if (originalName != null) {
                originalName = originalName.replace('\\', '/');
                originalName = originalName.substring(originalName.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
                if (originalName.length() > 255) originalName = originalName.substring(0, 255);
            }
            cloudFile.setOriginalName(originalName == null || originalName.isEmpty() ? label : originalName);
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
            return BaseRes.failure("读取上传" + label + "失败");
        } catch (RuntimeException exception) {
            return BaseRes.failure("保存上传" + label + "失败");
        }
    }

    /**
     * 根据文件编号查询图片内容。
     *
     * @param fileKey 文件编号，空值直接视为不存在
     * @return 文件实体，不存在时返回空值
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     */
    @Override
    public CloudFile getFile(String fileKey) {
        if (fileKey == null || fileKey.trim().isEmpty()) {
            return null;
        }
        // 按随机文件编号查询，避免使用原始文件名构造本地路径。
        return cloudFileMapper.selectOne(new QueryWrapper<CloudFile>().eq("file_key", fileKey));
    }

    /**
     * 删除指定随机编号的云端文件记录和二进制内容。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param fileKey 已校验归属的文件编号
     * @return 删除的文件条数
     */
    @Override
    public int deleteFile(String fileKey) {
        if (fileKey == null || fileKey.trim().isEmpty()) return 0;
        // 通过文件随机编号定位云端文件，不访问服务器本地磁盘。
        return cloudFileMapper.delete(new QueryWrapper<CloudFile>().eq("file_key", fileKey));
    }
}
