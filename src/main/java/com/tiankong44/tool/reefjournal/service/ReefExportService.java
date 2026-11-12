package com.tiankong44.tool.reefjournal.service;

import com.tiankong44.tool.reefjournal.dto.ReefBiologyDto;
import com.tiankong44.tool.reefjournal.dto.ReefBiologyEventDto;
import com.tiankong44.tool.reefjournal.dto.ReefEquipmentDto;
import com.tiankong44.tool.reefjournal.dto.ReefExportResponse;
import com.tiankong44.tool.reefjournal.dto.ReefLogDto;
import com.tiankong44.tool.reefjournal.dto.ReefPageResponse;
import com.tiankong44.tool.reefjournal.dto.ReefPhotoDto;
import com.tiankong44.tool.reefjournal.dto.ReefTankDto;
import com.tiankong44.tool.reefjournal.support.ReefDateTimeSupport;
import com.tiankong44.tool.reefjournal.support.ReefPageSupport;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.tiankong44.tool.reefjournal.ReefJournalConstants.MAX_PAGE_SIZE;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.ONE;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.ZERO;

/**
 * 组装当前用户的 JSON 备份数据，不包含照片二进制文件。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Service
public class ReefExportService {
    private final ReefTankService tankService;
    private final ReefBiologyService biologyService;
    private final ReefLogService logService;
    private final ReefEquipmentService equipmentService;
    private final ReefPhotoService photoService;
    private final ReefReminderService reminderService;
    private final ReefBackupExportDataService backupExportDataService;

    /**
     * 创建只导出当前用户数据的 JSON 组装服务。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param tankService 鱼缸查询服务
     * @param biologyService 生物事件查询服务
     * @param logService 日志查询服务
     * @param equipmentService 设备查询服务
     * @param photoService 照片元数据查询服务
     * @param reminderService 提醒配置查询服务
     */
    public ReefExportService(ReefTankService tankService, ReefBiologyService biologyService, ReefLogService logService,
                             ReefEquipmentService equipmentService, ReefPhotoService photoService, ReefReminderService reminderService,
                             ReefBackupExportDataService backupExportDataService) {
        this.tankService = tankService;
        this.biologyService = biologyService;
        this.logService = logService;
        this.equipmentService = equipmentService;
        this.photoService = photoService;
        this.reminderService = reminderService;
        this.backupExportDataService = backupExportDataService;
    }

    /**
     * 逐页读取当前用户记录并组装备份结构。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 认证上下文中的用户编号
     * @return 当前用户全部可导出资料
     */
    public ReefExportResponse export(Long userId) {
        ReefExportResponse result = new ReefExportResponse();
        result.setExportedAt(ReefDateTimeSupport.formatDateTime(ReefDateTimeSupport.now()));
        result.setBackupId(java.util.UUID.randomUUID().toString());
        List<ReefTankDto> tanks = readTanks(userId);
        result.setTanks(tanks);
        result.setBiology(readBiology(userId, tanks));
        result.setBiologyEvents(biologyService.eventsForUser(userId));
        result.setLogs(readLogs(userId));
        result.setEquipment(readEquipment(userId));
        result.setPhotos(readPhotos(userId));
        result.setWaterRanges(backupExportDataService.ranges(userId));
        result.setReminders(reminderService.get(userId));
        return result;
    }

    /**
     * 逐页读取当前用户全部鱼缸。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 当前用户编号
     * @return 鱼缸资料集合
     */
    private List<ReefTankDto> readTanks(Long userId) {
        List<ReefTankDto> result = new ArrayList<>();
        int pageNum = ONE;
        while (true) {
            ReefPageResponse<ReefTankDto> page = tankService.list(userId, pageNum, MAX_PAGE_SIZE);
            result.addAll(page.getList());
            if (result.size() >= Long.parseLong(page.getTotal()) || page.getList().isEmpty()) break;
            pageNum++;
        }
        return result;
    }

    /**
     * 逐缸读取生物资料及其事件时间轴。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 当前用户编号
     * @param tanks 当前用户鱼缸集合
     * @return 当前用户生物资料集合
     */
    private List<ReefBiologyDto> readBiology(Long userId, List<ReefTankDto> tanks) {
        List<ReefBiologyDto> allBiology = new ArrayList<>();
        for (ReefTankDto tank : tanks) {
            int pageNum = ONE;
            int tankBiologyCount = ZERO;
            while (true) {
                ReefPageResponse<ReefBiologyDto> page = biologyService.list(userId, Long.valueOf(tank.getId()), pageNum, MAX_PAGE_SIZE);
                allBiology.addAll(page.getList());
                tankBiologyCount += page.getList().size();
                if (tankBiologyCount >= Long.parseLong(page.getTotal()) || page.getList().isEmpty()) break;
                pageNum++;
            }
        }
        return allBiology;
    }

    /**
     * 逐页读取当前用户日志。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 当前用户编号
     * @return 当前用户日志集合
     */
    private List<ReefLogDto> readLogs(Long userId) {
        List<ReefLogDto> result = new ArrayList<>();
        int pageNum = ONE;
        while (true) {
            ReefPageResponse<ReefLogDto> page = logService.list(userId, null, null, pageNum, MAX_PAGE_SIZE);
            // 同一页批量补齐关联及测量值，保留可恢复的数据关系。
            backupExportDataService.populateLogs(userId, page.getList());
            result.addAll(page.getList());
            if (result.size() >= Long.parseLong(page.getTotal()) || page.getList().isEmpty()) break;
            pageNum++;
        }
        return result;
    }

    /**
     * 逐页读取当前用户设备。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 当前用户编号
     * @return 当前用户设备集合
     */
    private List<ReefEquipmentDto> readEquipment(Long userId) {
        List<ReefEquipmentDto> result = new ArrayList<>();
        int pageNum = ONE;
        while (true) {
            ReefPageResponse<ReefEquipmentDto> page = equipmentService.list(userId, null, null, pageNum, MAX_PAGE_SIZE);
            result.addAll(page.getList());
            if (result.size() >= Long.parseLong(page.getTotal()) || page.getList().isEmpty()) break;
            pageNum++;
        }
        return result;
    }

    /**
     * 逐页读取当前用户照片元数据。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 当前用户编号
     * @return 当前用户照片集合
     */
    private List<ReefPhotoDto> readPhotos(Long userId) {
        List<ReefPhotoDto> result = new ArrayList<>();
        int pageNum = ONE;
        while (true) {
            // 导出影像元数据时不添加关联对象过滤条件。
            ReefPageResponse<ReefPhotoDto> page = photoService.list(userId, null, null, null, pageNum, MAX_PAGE_SIZE);
            result.addAll(page.getList());
            if (result.size() >= Long.parseLong(page.getTotal()) || page.getList().isEmpty()) break;
            pageNum++;
        }
        for (ReefPhotoDto photo : result) photo.setContentUrl(null);
        return result;
    }
}
