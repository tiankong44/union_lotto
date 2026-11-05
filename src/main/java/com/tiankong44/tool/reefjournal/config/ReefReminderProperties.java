package com.tiankong44.tool.reefjournal.config;

import lombok.Data;

/**
 * 海缸提醒调度和微信访问令牌缓存配置。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
public class ReefReminderProperties {
    private String dispatchCron;
    private Long lockLeaseSeconds;
    private Long accessTokenCacheSeconds;
}
