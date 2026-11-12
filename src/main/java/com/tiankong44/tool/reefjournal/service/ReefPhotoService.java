package com.tiankong44.tool.reefjournal.service;

import com.tiankong44.tool.base.entity.BaseRes;
import com.tiankong44.tool.common.dto.CloudFileUploadResponse;
import com.tiankong44.tool.common.entity.CloudFile;
import com.tiankong44.tool.common.service.CommonService;
import com.tiankong44.tool.reefjournal.dto.ReefPageResponse;
import com.tiankong44.tool.reefjournal.dto.ReefPhotoDto;
import com.tiankong44.tool.reefjournal.dto.ReefPhotoSaveRequest;
import com.tiankong44.tool.reefjournal.exception.ReefJournalException;
import com.tiankong44.tool.reefjournal.auth.ReefPhotoTokenService;
import com.tiankong44.tool.reefjournal.support.ReefDateTimeSupport;
import com.tiankong44.tool.reefjournal.support.ReefPageSupport;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import static com.tiankong44.tool.reefjournal.ReefJournalConstants.ONE;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.ZERO;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.PHOTO_CATEGORY_TANK;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.PHOTO_CATEGORY_FISH;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.PHOTO_CATEGORY_CORAL;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.PHOTO_CATEGORY_EQUIPMENT;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.PHOTO_CATEGORY_PLANT;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.PHOTO_REFERENCE_BIOLOGY;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.PHOTO_REFERENCE_LOG;

/**
 * 管理用户照片元数据，并在读取二进制内容前检查照片归属。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Service
public class ReefPhotoService {
    private final JdbcTemplate jdbcTemplate;
    private final CommonService commonService;
    private final ReefTankService tankService;
    private final ReefPhotoTokenService photoTokenService;

    /**
     * 创建复用公共云端文件服务的照片业务服务。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param jdbcTemplate Spring 管理的数据库访问器
     * @param commonService 现有 MySQL 云端文件服务
     * @param tankService 用于验证关联对象归属
     */
    public ReefPhotoService(JdbcTemplate jdbcTemplate, CommonService commonService, ReefTankService tankService, ReefPhotoTokenService photoTokenService) {
        this.jdbcTemplate = jdbcTemplate;
        this.commonService = commonService;
        this.tankService = tankService;
        this.photoTokenService = photoTokenService;
    }

    /**
     * 按用户与照片分类分页读取影像记录。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 认证上下文中的用户编号
     * @param category 可选照片分类
     * @param referenceType 可选关联对象类型
     * @param referenceId 可选关联对象编号
     * @param requestedPage 客户端页码
     * @param requestedSize 客户端每页条数
     * @return 照片墙分页响应
     */
    public ReefPageResponse<ReefPhotoDto> list(Long userId, String category, String referenceType, String referenceId, Integer requestedPage, Integer requestedSize) {
        int pageNum = ReefPageSupport.pageNumber(requestedPage);
        int pageSize = ReefPageSupport.pageSize(requestedSize);
        StringBuilder filter = new StringBuilder(" WHERE user_id = ?");
        List<Object> parameters = new ArrayList<>();
        parameters.add(userId);
        if (category != null && !category.trim().isEmpty()) {
            filter.append(" AND category = ?");
            parameters.add(category);
        }
        if (referenceType != null && !referenceType.trim().isEmpty()) {
            validateReference(userId, referenceType, referenceId);
            filter.append(" AND reference_type = ? AND reference_id = ?");
            parameters.add(referenceType);
            parameters.add(referenceId.trim());
        }
        // 使用相同的归属与筛选条件计算分页总数。
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reef_photo" + filter, Long.class, parameters.toArray());
        parameters.add(pageSize);
        parameters.add(ReefPageSupport.offset(pageNum, pageSize));
        String sql = photoSelect() + filter + " ORDER BY recorded_at DESC, id DESC LIMIT ? OFFSET ?";
        // 使用参数化条件读取当前页影像，编号和筛选值不拼入 SQL。
        List<ReefPhotoDto> rows = jdbcTemplate.query(sql, BeanPropertyRowMapper.newInstance(ReefPhotoDto.class), parameters.toArray());
        for (ReefPhotoDto row : rows) {
            // 同一照片签名保护静态画面和对应动态片段。
            signContentUrls(userId, row);
        }
        return ReefPageSupport.response(pageNum, pageSize, total == null ? ZERO : total, rows);
    }

    /**
     * 使用现有云端文件服务保存图片，再记录当前用户的照片归属。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 认证上下文中的用户编号
     * @param file 用户选择的图片文件
     * @param title 照片名称
     * @param category 照片分类编码
     * @param referenceType 可选关联对象类型
     * @param referenceId 可选关联对象编号
     * @param recordedAt 可选上海本地记录时间
     * @param note 可选照片备注
     * @return 受保护内容地址的照片元数据
     */
    public ReefPhotoDto upload(Long userId, MultipartFile file, String title, String category, String referenceType, String referenceId, String recordedAt, String note) {
        validateCategory(category);
        validateReference(userId, referenceType, referenceId);
        // 复用既有文件服务在 MySQL 中保存图片，不写入实例本地磁盘。
        BaseRes<CloudFileUploadResponse> uploadResult = commonService.uploadIcon(file);
        if (uploadResult == null || uploadResult.getCode() != ZERO || uploadResult.getData() == null) {
            throw new ReefJournalException(uploadResult == null ? "照片暂时没有保存，请重试" : uploadResult.getMessage());
        }
        CloudFileUploadResponse fileResponse = uploadResult.getData();
        // 使用上海本地时间保存照片记录，未传入时间时取当前时刻。
        java.time.LocalDateTime photoTime = recordedAt == null || recordedAt.trim().isEmpty()
                ? ReefDateTimeSupport.now() : ReefDateTimeSupport.parseDateTime(recordedAt);
        jdbcTemplate.update("INSERT INTO reef_photo (user_id, file_key, category, title, reference_type, reference_id, recorded_at, note, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())",
                userId, fileResponse.getFileKey(), category, cleanTitle(title), normalizeReference(referenceType), normalizeReference(referenceId), photoTime, cleanNote(note));
        List<ReefPhotoDto> rows = jdbcTemplate.query(photoSelect() + " WHERE user_id = ? AND file_key = ? ORDER BY id DESC LIMIT 1",
                BeanPropertyRowMapper.newInstance(ReefPhotoDto.class), userId, fileResponse.getFileKey());
        if (rows.isEmpty()) throw new ReefJournalException("照片元数据没有保存，请重试");
        return rows.get(ZERO);
    }

    /**
     * 读取当前用户的一条影像记录和受保护图片地址。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 认证上下文中的用户编号
     * @param photoId 照片编号
     * @return 照片元数据与签名内容地址
     */
    public ReefPhotoDto get(Long userId, Long photoId) {
        // 通过用户编号和照片编号定位详情记录。
        List<ReefPhotoDto> rows = jdbcTemplate.query(photoSelect() + " WHERE user_id = ? AND id = ? LIMIT 1",
                BeanPropertyRowMapper.newInstance(ReefPhotoDto.class), userId, photoId);
        if (rows.isEmpty()) throw new ReefJournalException("没有找到这张照片，或它不属于当前账号");
        ReefPhotoDto photo = rows.get(ZERO);
        // 返回静态画面和可选动态片段的短时访问地址。
        signContentUrls(userId, photo);
        return photo;
    }

    /**
     * 将实况照片的动态片段存入共享文件表并绑定到所属照片记录。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-12
     * @param userId 认证上下文中的用户编号
     * @param photoId 照片编号
     * @param file 用户选择的 MOV 或 MP4 动态片段
     * @return 更新后的照片资料
     */
    public ReefPhotoDto uploadMotion(Long userId, Long photoId, MultipartFile file) {
        String currentMotionKey = photoMotionKey(userId, photoId);
        if (currentMotionKey != null && !currentMotionKey.trim().isEmpty()) return get(userId, photoId);
        // 上传前由公共文件服务校验视频类型和单文件大小。
        BaseRes<CloudFileUploadResponse> uploadResult = commonService.uploadVideo(file);
        if (uploadResult == null || uploadResult.getCode() != ZERO || uploadResult.getData() == null) {
            throw new ReefJournalException(uploadResult == null ? "动态片段没有保存，请重试" : uploadResult.getMessage());
        }
        String fileKey = uploadResult.getData().getFileKey();
        int updatedCount;
        try {
            // 只允许当前用户为尚未绑定动态片段的照片添加文件。
            updatedCount = jdbcTemplate.update("UPDATE reef_photo SET motion_file_key = ? WHERE user_id = ? AND id = ? AND motion_file_key IS NULL",
                    fileKey, userId, photoId);
        } catch (RuntimeException exception) {
            // 绑定失败时清理刚写入的共享文件，避免留下无归属文件。
            commonService.deleteFile(fileKey);
            throw exception;
        }
        if (updatedCount != ONE) {
            // 并发请求已有其他片段完成绑定时，移除本次未使用的文件。
            commonService.deleteFile(fileKey);
            throw new ReefJournalException("动态片段没有绑定，请重新读取照片后重试");
        }
        return get(userId, photoId);
    }

    /**
     * 更新当前用户影像的分类、关联对象、时间和备注。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 认证上下文中的用户编号
     * @param photoId 照片编号
     * @param request 用户修改后的影像资料
     * @return 保存后的影像详情
     */
    public ReefPhotoDto update(Long userId, Long photoId, ReefPhotoSaveRequest request) {
        validateCategory(request.getCategory());
        String referenceType = normalizeReference(request.getReferenceType());
        String referenceId = normalizeReference(request.getReferenceId());
        validateReference(userId, referenceType, referenceId);
        java.time.LocalDateTime recordedAt = ReefDateTimeSupport.parseDateTime(request.getRecordedAt());
        // 按用户编号和照片编号同时限定更新范围。
        int updatedCount = jdbcTemplate.update("UPDATE reef_photo SET category = ?, title = ?, reference_type = ?, reference_id = ?, recorded_at = ?, note = ? WHERE user_id = ? AND id = ?",
                request.getCategory(), cleanTitle(request.getTitle()), referenceType, referenceId, recordedAt,
                cleanNote(request.getNote()), userId, photoId);
        if (updatedCount != ONE) throw new ReefJournalException("没有找到这张照片，或它不属于当前账号");
        return get(userId, photoId);
    }

    /**
     * 删除当前用户的影像元数据、静态画面及对应动态片段。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 认证上下文中的用户编号
     * @param photoId 照片编号
     */
    @Transactional
    public void delete(Long userId, Long photoId) {
        List<String> fileKeys = jdbcTemplate.query("SELECT file_key FROM reef_photo WHERE user_id = ? AND id = ? LIMIT 1",
                (resultSet, rowNumber) -> resultSet.getString(1), userId, photoId);
        if (fileKeys.isEmpty()) throw new ReefJournalException("没有找到这张照片，或它不属于当前账号");
        String motionFileKey = photoMotionKey(userId, photoId);
        // 先删除用户拥有的照片记录，再删除该记录对应的云端文件。
        int deletedPhotoCount = jdbcTemplate.update("DELETE FROM reef_photo WHERE user_id = ? AND id = ?", userId, photoId);
        if (deletedPhotoCount != ONE) throw new ReefJournalException("照片没有删除，请刷新后重试");
        int deletedFileCount = commonService.deleteFile(fileKeys.get(ZERO));
        if (deletedFileCount != ONE) throw new ReefJournalException("照片记录已找到，但云端文件无法删除");
        if (motionFileKey != null && !motionFileKey.trim().isEmpty() && commonService.deleteFile(motionFileKey) != ONE) {
            throw new ReefJournalException("照片已删除，但动态片段文件没有清理");
        }
    }

    /**
     * 只有照片确属当前用户时才读取云端二进制内容。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 认证上下文中的用户编号
     * @param photoId 照片编号
     * @return 云端文件实体
     */
    public CloudFile content(Long userId, Long photoId) {
        List<String> fileKeys = jdbcTemplate.query("SELECT file_key FROM reef_photo WHERE user_id = ? AND id = ? LIMIT 1",
                (resultSet, rowNumber) -> resultSet.getString(1), userId, photoId);
        if (fileKeys.isEmpty()) throw new ReefJournalException("没有找到这张照片，或它不属于当前账号");
        CloudFile cloudFile = commonService.getFile(fileKeys.get(ZERO));
        if (cloudFile == null) throw new ReefJournalException("照片文件暂时无法读取");
        return cloudFile;
    }

    /**
     * 只有照片确属当前用户时才读取对应的动态片段。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-12
     * @param userId 认证上下文中的用户编号
     * @param photoId 照片编号
     * @return 云端视频文件实体
     */
    public CloudFile motionContent(Long userId, Long photoId) {
        String motionFileKey = photoMotionKey(userId, photoId);
        if (motionFileKey == null || motionFileKey.trim().isEmpty()) throw new ReefJournalException("这张照片没有动态片段");
        // 通过照片关联的随机文件编号读取共享云端内容。
        CloudFile cloudFile = commonService.getFile(motionFileKey);
        if (cloudFile == null) throw new ReefJournalException("动态片段暂时无法读取");
        return cloudFile;
    }

    /**
     * 读取当前用户照片所关联的动态片段文件编号。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-12
     * @param userId 当前用户编号
     * @param photoId 照片编号
     * @return 动态片段文件编号，没有片段时返回空值
     */
    private String photoMotionKey(Long userId, Long photoId) {
        List<String> motionKeys = jdbcTemplate.query("SELECT motion_file_key FROM reef_photo WHERE user_id = ? AND id = ? LIMIT 1",
                (resultSet, rowNumber) -> resultSet.getString(1), userId, photoId);
        if (motionKeys.isEmpty()) throw new ReefJournalException("没有找到这张照片，或它不属于当前账号");
        return motionKeys.get(ZERO);
    }

    /**
     * 为静态画面和可选动态片段附加同一张照片的短时签名。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-12
     * @param userId 当前照片所属用户
     * @param photo 照片响应资料
     */
    private void signContentUrls(Long userId, ReefPhotoDto photo) {
        // 只为当前用户拥有的照片编号签发有时限的媒体访问凭证。
        String signature = photoTokenService.issue(userId, Long.valueOf(photo.getId()));
        photo.setContentUrl(photo.getContentUrl() + "?signature=" + signature);
        if (photo.getMotionContentUrl() != null) photo.setMotionContentUrl(photo.getMotionContentUrl() + "?signature=" + signature);
    }

    /**
     * 校验照片分类只使用页面约定值。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param category 用户提交的照片分类
     */
    private void validateCategory(String category) {
        if (!Arrays.asList(PHOTO_CATEGORY_TANK, PHOTO_CATEGORY_FISH, PHOTO_CATEGORY_CORAL, PHOTO_CATEGORY_EQUIPMENT, PHOTO_CATEGORY_PLANT,
                com.tiankong44.tool.reefjournal.ReefJournalConstants.PHOTO_CATEGORY_OTHER).contains(category)) {
            throw new ReefJournalException("照片分类不受支持");
        }
    }

    /**
     * 校验照片关联对象属于当前用户，避免跨账号建立引用。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 当前用户编号
     * @param referenceType 关联对象类型
     * @param referenceId 关联对象编号
     */
    private void validateReference(Long userId, String referenceType, String referenceId) {
        if (referenceType == null || referenceType.trim().isEmpty()) return;
        if (referenceId == null || referenceId.trim().isEmpty()) throw new ReefJournalException("照片关联编号不能为空");
        Long id = tankService.parseId(referenceId);
        String table = referenceTable(referenceType);
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE user_id = ? AND id = ?", Integer.class, userId, id);
        if (count == null || count != ONE) throw new ReefJournalException("照片关联对象不属于当前账号");
    }

    /**
     * 从固定表名白名单中取得照片关联对象表。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param referenceType 关联对象类型编码
     * @return 固定表名白名单项
     */
    private String referenceTable(String referenceType) {
        if (com.tiankong44.tool.reefjournal.ReefAquariumConstants.PHOTO_REFERENCE_EVENT.equals(referenceType)) return "reef_biology_event";
        if (PHOTO_CATEGORY_TANK.equals(referenceType)) return "reef_tank";
        if (PHOTO_REFERENCE_BIOLOGY.equals(referenceType)) return "reef_biology";
        if (PHOTO_CATEGORY_EQUIPMENT.equals(referenceType)) return "reef_equipment";
        if (PHOTO_REFERENCE_LOG.equals(referenceType)) return "reef_log";
        throw new ReefJournalException("照片关联类型不受支持");
    }

    /**
     * 将可选关联字段中的空文本统一转换为空值。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param value 用户输入的关联字段
     * @return 去除首尾空白后的文本，空值时返回 null
     */
    private String normalizeReference(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    /**
     * 清理照片标题中的控制字符并限制数据库长度。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param title 用户提供的照片标题
     * @return 清理后的标题
     */
    private String cleanTitle(String title) {
        if (title == null) return null;
        String cleaned = title.replaceAll("[\\p{Cntrl}]", "").trim();
        return cleaned.length() > 255 ? cleaned.substring(ZERO, 255) : cleaned;
    }

    /**
     * 清理影像备注并限制数据库字段长度。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param note 用户填写的影像备注
     * @return 清理后的备注或空值
     */
    private String cleanNote(String note) {
        if (note == null) return null;
        String cleaned = note.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "").trim();
        return cleaned.length() > 2000 ? cleaned.substring(ZERO, 2000) : cleaned;
    }

    /**
     * 返回照片墙响应所需的固定字段投影。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @return 参数化查询前缀
     */
    private String photoSelect() {
        return "SELECT CAST(reef_photo.id AS CHAR) id, title, category, reference_type, reference_id, DATE_FORMAT(recorded_at, '%Y-%m-%d %H:%i:%s') recorded_at, note, CONCAT('/tabs/reef-journal/photos/', CAST(reef_photo.id AS CHAR), '/content') content_url, CASE WHEN motion_file_key IS NULL THEN NULL ELSE CONCAT('/tabs/reef-journal/photos/', CAST(reef_photo.id AS CHAR), '/motion-content') END motion_content_url FROM reef_photo";
    }
}
