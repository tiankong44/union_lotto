package com.tiankong44.tool.reefjournal.controller;

import com.tiankong44.tool.base.entity.BaseRes;
import com.tiankong44.tool.common.entity.CloudFile;
import com.tiankong44.tool.reefjournal.ReefJournalConstants;
import com.tiankong44.tool.reefjournal.auth.ReefPhotoTokenService;
import com.tiankong44.tool.reefjournal.dto.ReefPhotoDto;
import com.tiankong44.tool.reefjournal.service.ReefPhotoService;
import com.tiankong44.tool.reefjournal.service.ReefTankService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/**
 * 接收并读取已绑定照片的实况动态片段。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
@RestController
@RequestMapping("/reef-journal/photos")
public class ReefPhotoMotionController {
    private final ReefPhotoService photoService;
    private final ReefTankService tankService;
    private final ReefPhotoTokenService photoTokenService;

    /** 创建复用照片归属校验和短时签名的动态片段接口。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    public ReefPhotoMotionController(ReefPhotoService photoService, ReefTankService tankService, ReefPhotoTokenService photoTokenService) {
        this.photoService = photoService;
        this.tankService = tankService;
        this.photoTokenService = photoTokenService;
    }

    /**
     * 将实况照片动态片段绑定到当前用户拥有的静态照片记录。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-12
     * @param userId 已认证用户编号
     * @param photoId 照片字符串编号
     * @param file MOV 或 MP4 动态片段
     * @return 更新后的照片和动效内容地址
     */
    @PostMapping(value = "/{photoId}/motion", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseRes<ReefPhotoDto> uploadMotion(@RequestAttribute(ReefJournalConstants.REQUEST_USER_ID) Long userId,
                                             @PathVariable String photoId,
                                             @RequestParam("file") MultipartFile file) {
        // 先解析字符串编号，再由照片服务校验用户归属并保存片段。
        return BaseRes.success(photoService.uploadMotion(userId, tankService.parseId(photoId), file));
    }

    /**
     * 验证照片签名后读取对应的动态片段。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-12
     * @param photoId 照片字符串编号
     * @param signature 短时照片访问签名
     * @return 视频字节流和实际媒体类型
     */
    @GetMapping("/{photoId}/motion-content")
    public ResponseEntity<byte[]> motionContent(@PathVariable String photoId, @RequestParam String signature) {
        // 先验证签名并取得照片归属用户，再读取动态文件。
        Long userId = photoTokenService.verify(photoId, signature);
        if (userId == null) return ResponseEntity.status(401).build();
        // 先校验当前用户对照片的归属，再读取共享云端视频文件。
        CloudFile file = photoService.motionContent(userId, tankService.parseId(photoId));
        MediaType mediaType = parseMediaType(file.getContentType());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentLength(file.getFileSize() == null ? 0L : file.getFileSize());
        headers.setContentDisposition(ContentDisposition.inline().filename(file.getFileKey(), StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(file.getFileContent());
    }

    /**
     * 解析已通过上传白名单校验的媒体类型。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-12
     * @param contentType 云端文件媒体类型
     * @return HTTP 响应使用的媒体类型
     */
    private MediaType parseMediaType(String contentType) {
        if (contentType == null) return MediaType.APPLICATION_OCTET_STREAM;
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
