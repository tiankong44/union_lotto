package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户提醒偏好查询和保存模型。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
public class ReefReminderSettingsDto {
    private String sendTime;
    @Valid
    private List<ReefReminderItemDto> items = new ArrayList<>();
}
