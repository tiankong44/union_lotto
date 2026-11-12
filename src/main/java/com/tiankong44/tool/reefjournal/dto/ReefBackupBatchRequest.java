package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

/**
 * 备份恢复的单批数据；按鱼缸、生物、事件、日志和设备顺序提交。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
@Data
public class ReefBackupBatchRequest {
    @NotBlank(message = "备份编号不能为空")
    private String backupId;
    @Valid private List<ReefTankDto> tanks = new ArrayList<>();
    @Valid private List<ReefBiologyDto> biology = new ArrayList<>();
    @Valid private List<ReefBiologyEventDto> biologyEvents = new ArrayList<>();
    @Valid private List<ReefLogDto> logs = new ArrayList<>();
    @Valid private List<ReefEquipmentDto> equipment = new ArrayList<>();
    @Valid private List<ReefBackupRangeDto> waterRanges = new ArrayList<>();
}
