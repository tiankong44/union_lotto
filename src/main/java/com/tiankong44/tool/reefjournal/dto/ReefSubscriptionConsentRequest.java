package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 小程序用户本次订阅授权结果。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
public class ReefSubscriptionConsentRequest {
    @NotBlank(message = "提醒类型不能为空")
    private String type;
    @NotNull(message = "授权结果不能为空")
    private Boolean accepted;
}
