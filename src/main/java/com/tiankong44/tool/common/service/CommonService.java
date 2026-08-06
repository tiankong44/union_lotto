package com.tiankong44.tool.common.service;

import com.tiankong44.tool.base.entity.BaseRes;
import com.tiankong44.tool.common.dto.CloudFileUploadResponse;
import com.tiankong44.tool.common.entity.CloudFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * 公共文件服务接口，定义云端文件上传和读取能力。
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
     * 按文件编号读取云端图片内容。
     *
     * @param fileKey 文件编号
     * @return 文件实体，不存在时返回空值
     */
    CloudFile getFile(String fileKey);
}
