package com.tiankong44.tool.reefjournal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 小程序登录成功响应。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReefLoginResponse {
    private String accessToken;
    private String userId;
}
