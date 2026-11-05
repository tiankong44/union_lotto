package com.tiankong44.tool.reefjournal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 水境日记微信登录和订阅提醒配置。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
@ConfigurationProperties(prefix = "reef-journal")
public class ReefJournalProperties {
    private String appId;
    private String appSecret;
    private String tokenSigningKey;
    private Long tokenTtlSeconds;
    private Long photoUrlTtlSeconds;
    private String waterChangeTemplateId;
    private String waterTestTemplateId;
    private String feedingTemplateId;
    private String equipmentTemplateId;
    private String waterChangeTitleField;
    private String waterChangeTimeField;
    private String waterTestTitleField;
    private String waterTestTimeField;
    private String feedingTitleField;
    private String feedingTimeField;
    private String equipmentTitleField;
    private String equipmentTimeField;
    private ReefReminderProperties reminder;
}
