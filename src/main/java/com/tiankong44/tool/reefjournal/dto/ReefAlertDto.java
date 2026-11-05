package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

/**
 * 用户自设范围触发的水质提醒。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
public class ReefAlertDto {
    private String id;
    private String level;
    private String message;
}
