package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 小程序登录凭证请求。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
public class ReefLoginRequest {
    @NotBlank(message = "微信登录凭证不能为空")
    @Size(max = 256, message = "微信登录凭证长度无效")
    private String code;
}
