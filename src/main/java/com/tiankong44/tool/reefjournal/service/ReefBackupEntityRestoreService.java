package com.tiankong44.tool.reefjournal.service;

import com.tiankong44.tool.reefjournal.dto.*;
import org.springframework.stereotype.Service;
import java.util.List;
import static com.tiankong44.tool.reefjournal.ReefAquariumConstants.BACKUP_TANK;
import static com.tiankong44.tool.reefjournal.ReefAquariumConstants.BACKUP_BIOLOGY;
import static com.tiankong44.tool.reefjournal.ReefAquariumConstants.BACKUP_EQUIPMENT;

/**
 * 将备份中的鱼缸、生物与设备恢复到当前账号。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
@Service
public class ReefBackupEntityRestoreService {
    private final ReefTankService tanks;
    private final ReefBiologyService biology;
    private final ReefEquipmentService equipment;
    private final ReefBackupValidationService validation;

    /** 创建复用现有领域服务的恢复处理器。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    public ReefBackupEntityRestoreService(ReefTankService tanks, ReefBiologyService biology, ReefEquipmentService equipment, ReefBackupValidationService validation) {
        this.tanks = tanks;
        this.biology = biology;
        this.equipment = equipment;
        this.validation = validation;
    }

    /** 恢复鱼缸并登记备份编号，重复批次跳过已恢复记录。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    public void tanks(Long userId, List<ReefTankDto> rows, ReefBackupRestoreContext context) {
        for (ReefTankDto row : rows) {
            if (context.contains(BACKUP_TANK, row.getId())) continue;
            ReefTankSaveRequest request = new ReefTankSaveRequest();
            request.setName(row.getName()); request.setStartedAt(row.getStartedAt());
            request.setCapacityLiter(validation.decimal(row.getCapacityLiter())); request.setSystemType(row.getSystemType());
            request.setNote(row.getNote()); request.setWaterType(row.getWaterType()); request.setThemes(row.getThemes());
            request.setEnabledMetrics(row.getEnabledMetrics()); request.setArchived(row.getArchived());
            validation.validate(request);
            // 调用现有鱼缸服务创建当前账号资料。
            context.add(BACKUP_TANK, row.getId(), tanks.save(userId, request).getId());
        }
    }

    /** 恢复生物当前资料，原事件在后续批次按时间原样写入。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    public void biology(Long userId, List<ReefBiologyDto> rows, ReefBackupRestoreContext context) {
        for (ReefBiologyDto row : rows) {
            if (context.contains(BACKUP_BIOLOGY, row.getId())) continue;
            ReefBiologySaveRequest request = new ReefBiologySaveRequest();
            request.setTankId(context.require(BACKUP_TANK, row.getTankId())); request.setName(row.getName());
            request.setScientificName(row.getScientificName()); request.setCategory(row.getCategory()); request.setAddedAt(row.getAddedAt());
            request.setCurrentTankSince(row.getCurrentTankSince());
            request.setBodyLengthCm(validation.decimal(row.getBodyLengthCm())); request.setSource(row.getSource());
            request.setStatus(row.getStatus()); request.setTrackingMode(row.getTrackingMode()); request.setQuantity(row.getQuantity());
            request.setPositionNote(row.getPositionNote()); request.setNote(row.getNote());
            validation.validate(request);
            // 不另造入缸事件，后续批次会恢复备份内的真实事件。
            context.add(BACKUP_BIOLOGY, row.getId(), biology.restoreNew(userId, request).getId());
        }
    }

    /** 恢复设备和维护资料。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    public void equipment(Long userId, List<ReefEquipmentDto> rows, ReefBackupRestoreContext context) {
        for (ReefEquipmentDto row : rows) {
            if (context.contains(BACKUP_EQUIPMENT, row.getId())) continue;
            ReefEquipmentSaveRequest request = new ReefEquipmentSaveRequest();
            request.setTankId(row.getTankId() == null ? null : context.require(BACKUP_TANK, row.getTankId()));
            request.setCategory(row.getCategory()); request.setName(row.getName()); request.setModel(row.getModel());
            request.setStatus(row.getStatus()); request.setPowerWatt(validation.decimal(row.getPowerWatt()));
            request.setDailyHours(validation.decimal(row.getDailyHours())); request.setStartedAt(row.getStartedAt());
            request.setMaintenanceIntervalDays(row.getMaintenanceIntervalDays()); request.setLastMaintainedAt(row.getLastMaintainedAt());
            validation.validate(request);
            // 复用设备服务的状态和用户归属校验。
            context.add(BACKUP_EQUIPMENT, row.getId(), equipment.save(userId, request).getId());
        }
    }
}
