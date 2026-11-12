package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

/**
 * 当前水境日记用户资料响应。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
@Data
public class ReefUserProfileDto {
    private String nickname;
    private String avatarFileKey;
    private String avatarUrl;
}
