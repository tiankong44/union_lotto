package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户可下载的 JSON 备份数据，不包含照片二进制内容。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Data
public class ReefExportResponse {
    private String exportedAt;
    private List<ReefTankDto> tanks = new ArrayList<>();
    private List<ReefBiologyDto> biology = new ArrayList<>();
    private List<ReefBiologyEventDto> biologyEvents = new ArrayList<>();
    private List<ReefLogDto> logs = new ArrayList<>();
    private List<ReefEquipmentDto> equipment = new ArrayList<>();
    private List<ReefPhotoDto> photos = new ArrayList<>();
    private ReefReminderSettingsDto reminders;
}
