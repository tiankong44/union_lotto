package com.tiankong44.tool.reefjournal.service;

import com.tiankong44.tool.base.entity.BaseRes;
import com.tiankong44.tool.common.dto.CloudFileUploadResponse;
import com.tiankong44.tool.common.entity.CloudFile;
import com.tiankong44.tool.common.service.CommonService;
import com.tiankong44.tool.reefjournal.dto.ReefPageResponse;
import com.tiankong44.tool.reefjournal.dto.ReefPhotoDto;
import com.tiankong44.tool.reefjournal.exception.ReefJournalException;
import com.tiankong44.tool.reefjournal.auth.ReefPhotoTokenService;
import com.tiankong44.tool.reefjournal.support.ReefPageSupport;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Arrays;

import static com.tiankong44.tool.reefjournal.ReefJournalConstants.ONE;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.ZERO;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.PHOTO_CATEGORY_TANK;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.PHOTO_CATEGORY_FISH;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.PHOTO_CATEGORY_CORAL;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.PHOTO_CATEGORY_EQUIPMENT;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.PHOTO_CATEGORY_PLANT;

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
     * @param requestedPage 客户端页码
     * @param requestedSize 客户端每页条数
     * @return 照片墙分页响应
     */
    public ReefPageResponse<ReefPhotoDto> list(Long userId, String category, Integer requestedPage, Integer requestedSize) {
        int pageNum = ReefPageSupport.pageNumber(requestedPage);
        int pageSize = ReefPageSupport.pageSize(requestedSize);
        String filter = category == null || category.trim().isEmpty() ? "" : " AND category = ?";
        Long total = category == null || category.trim().isEmpty()
                ? jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reef_photo WHERE user_id = ?", Long.class, userId)
                : jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reef_photo WHERE user_id = ? AND category = ?", Long.class, userId, category);
        String sql = photoSelect() + " WHERE user_id = ?" + filter + " ORDER BY recorded_at DESC, id DESC LIMIT ? OFFSET ?";
        List<ReefPhotoDto> rows = category == null || category.trim().isEmpty()
                ? jdbcTemplate.query(sql, BeanPropertyRowMapper.newInstance(ReefPhotoDto.class), userId, pageSize, ReefPageSupport.offset(pageNum, pageSize))
                : jdbcTemplate.query(sql, BeanPropertyRowMapper.newInstance(ReefPhotoDto.class), userId, category, pageSize, ReefPageSupport.offset(pageNum, pageSize));
        for (ReefPhotoDto row : rows) {
            row.setContentUrl(row.getContentUrl() + "?signature=" + photoTokenService.issue(userId, Long.valueOf(row.getId())));
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
     * @return 受保护内容地址的照片元数据
     */
    public ReefPhotoDto upload(Long userId, MultipartFile file, String title, String category, String referenceType, String referenceId) {
        validateCategory(category);
        validateReference(userId, referenceType, referenceId);
        // 复用既有文件服务在 MySQL 中保存图片，不写入实例本地磁盘。
        BaseRes<CloudFileUploadResponse> uploadResult = commonService.uploadIcon(file);
        if (uploadResult == null || uploadResult.getCode() != ZERO || uploadResult.getData() == null) {
            throw new ReefJournalException(uploadResult == null ? "照片暂时没有保存，请重试" : uploadResult.getMessage());
        }
        CloudFileUploadResponse fileResponse = uploadResult.getData();
        jdbcTemplate.update("INSERT INTO reef_photo (user_id, file_key, category, title, reference_type, reference_id, recorded_at, created_at) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                userId, fileResponse.getFileKey(), category, cleanTitle(title), referenceType, referenceId);
        List<ReefPhotoDto> rows = jdbcTemplate.query(photoSelect() + " WHERE user_id = ? AND file_key = ? ORDER BY id DESC LIMIT 1",
                BeanPropertyRowMapper.newInstance(ReefPhotoDto.class), userId, fileResponse.getFileKey());
        if (rows.isEmpty()) throw new ReefJournalException("照片元数据没有保存，请重试");
        return rows.get(ZERO);
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
     * 校验照片分类只使用页面约定值。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param category 用户提交的照片分类
     */
    private void validateCategory(String category) {
        if (!Arrays.asList(PHOTO_CATEGORY_TANK, PHOTO_CATEGORY_FISH, PHOTO_CATEGORY_CORAL, PHOTO_CATEGORY_EQUIPMENT, PHOTO_CATEGORY_PLANT).contains(category)) {
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
        if (PHOTO_CATEGORY_TANK.equals(referenceType)) return "reef_tank";
        if ("BIOLOGY".equals(referenceType)) return "reef_biology";
        if (PHOTO_CATEGORY_EQUIPMENT.equals(referenceType)) return "reef_equipment";
        if ("LOG".equals(referenceType)) return "reef_log";
        throw new ReefJournalException("照片关联类型不受支持");
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
     * 返回照片墙响应所需的固定字段投影。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @return 参数化查询前缀
     */
    private String photoSelect() {
        return "SELECT CAST(id AS CHAR) id, title, category, reference_type, reference_id, DATE_FORMAT(recorded_at, '%Y-%m-%d %H:%i:%s') recorded_at, CONCAT('/tabs/reef-journal/photos/', CAST(id AS CHAR), '/content') content_url FROM reef_photo";
    }
}
