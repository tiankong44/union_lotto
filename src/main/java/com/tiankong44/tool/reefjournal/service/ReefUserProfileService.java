package com.tiankong44.tool.reefjournal.service;

import com.tiankong44.tool.reefjournal.ReefJournalConstants;
import com.tiankong44.tool.reefjournal.dto.ReefUserProfileDto;
import com.tiankong44.tool.reefjournal.dto.ReefUserProfileSaveRequest;
import com.tiankong44.tool.reefjournal.exception.ReefJournalException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 读取并保存经认证的水境日记用户资料。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
@Service
public class ReefUserProfileService {
    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建用户资料服务。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-12
     * @param jdbcTemplate 用户资料数据访问器
     */
    public ReefUserProfileService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 读取指定已认证账号的资料。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-12
     * @param userId 认证上下文中的用户编号
     * @return 昵称和头像访问地址
     */
    public ReefUserProfileDto getProfile(Long userId) {
        List<ReefUserProfileDto> profiles = jdbcTemplate.query(
                "SELECT nickname, avatar_file_key AS avatarFileKey FROM reef_user WHERE id = ? LIMIT 1",
                (resultSet, rowNumber) -> {
                    ReefUserProfileDto profile = new ReefUserProfileDto();
                    profile.setNickname(resultSet.getString("nickname"));
                    profile.setAvatarFileKey(resultSet.getString("avatarFileKey"));
                    String avatarFileKey = profile.getAvatarFileKey();
                    profile.setAvatarUrl(avatarFileKey == null ? null : ReefJournalConstants.PROFILE_AVATAR_CONTENT_PATH + avatarFileKey);
                    return profile;
                }, userId);
        if (profiles.isEmpty()) {
            throw new ReefJournalException("当前账号资料不存在，请重新登录");
        }
        return profiles.get(0);
    }

    /**
     * 校验云端头像文件后更新当前用户资料。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-12
     * @param userId 认证上下文中的用户编号
     * @param request 用户填写的资料
     * @return 保存后的资料
     */
    public ReefUserProfileDto saveProfile(Long userId, ReefUserProfileSaveRequest request) {
        String avatarFileKey = request.getAvatarFileKey();
        if (avatarFileKey != null && !avatarFileKey.trim().isEmpty()) {
            // 只检查云端图片元数据，避免为校验头像读取整段图片内容。
            Integer imageCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM cloud_file WHERE file_key = ? AND content_type LIKE 'image/%'",
                    Integer.class, avatarFileKey);
            if (imageCount == null || imageCount == 0) {
                throw new ReefJournalException("头像文件不存在或不是图片，请重新选择");
            }
        } else {
            avatarFileKey = null;
        }
        // 更新可能与现有值相同，随后读取资料可区分成功更新与账号缺失。
        jdbcTemplate.update("UPDATE reef_user SET nickname = ?, avatar_file_key = ? WHERE id = ?",
                request.getNickname().trim(), avatarFileKey, userId);
        return getProfile(userId);
    }
}
