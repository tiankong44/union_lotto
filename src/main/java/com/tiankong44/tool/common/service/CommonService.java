package com.tiankong44.tool.common.service;

import com.tiankong44.tool.base.entity.BaseRes;
import com.tiankong44.tool.common.dto.CloudFileUploadResponse;
import com.tiankong44.tool.common.entity.CloudFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * 公共文件服务接口，定义图片、视频上传及云端文件读取能力。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-08-06
 */
public interface CommonService {
    /**
     * 上传图片并将文件内容保存到云端 MySQL。
     *
     * @param file 待上传图片，不能为空且大小不能超过10MB
     * @return 文件访问信息，参数非法或保存失败时返回失败响应
     */
    BaseRes<CloudFileUploadResponse> uploadIcon(MultipartFile file);

    /**
     * 上传允许格式的视频文件并将内容保存到云端 MySQL。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-12
     * @param file 待上传的 MP4、MOV、M4V 或 3GP 视频
     * @return 文件访问信息，参数非法或保存失败时返回失败响应
     */
    BaseRes<CloudFileUploadResponse> uploadVideo(MultipartFile file);

    /**
     * 按文件编号读取云端图片或视频内容。
     *
     * @param fileKey 文件编号
     * @return 文件实体，不存在时返回空值
     */
    CloudFile getFile(String fileKey);

    /**
     * 按随机文件编号删除云端文件内容。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param fileKey 已校验归属且不再被照片记录引用的文件编号
     * @return 删除的文件条数
     */
    int deleteFile(String fileKey);
}
