package com.tiankong44.tool.reefjournal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 微信登录凭证校验接口的服务端响应。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
public class ReefWechatSessionResponse {
    private String openid;
    @JsonProperty("session_key")
    private String sessionKey;
    private Integer errcode;
    private String errmsg;
}
