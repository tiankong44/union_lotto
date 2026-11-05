package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

/**
 * 微信订阅消息发送接口响应。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
public class ReefWechatSendResponse {
    private Integer errcode;
    private String errmsg;
}
