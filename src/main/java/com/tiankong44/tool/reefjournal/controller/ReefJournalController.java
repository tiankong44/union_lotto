package com.tiankong44.tool.reefjournal.controller;

import com.tiankong44.tool.base.entity.BaseRes;
import com.tiankong44.tool.common.entity.CloudFile;
import com.tiankong44.tool.reefjournal.ReefJournalConstants;
import com.tiankong44.tool.reefjournal.auth.ReefPhotoTokenService;
import com.tiankong44.tool.reefjournal.dto.ReefBiologyDto;
import com.tiankong44.tool.reefjournal.dto.ReefBiologyEventDto;
import com.tiankong44.tool.reefjournal.dto.ReefBiologyEventSaveRequest;
import com.tiankong44.tool.reefjournal.dto.ReefBiologySaveRequest;
import com.tiankong44.tool.reefjournal.dto.ReefDashboardResponse;
import com.tiankong44.tool.reefjournal.dto.ReefEquipmentDto;
import com.tiankong44.tool.reefjournal.dto.ReefEquipmentSaveRequest;
import com.tiankong44.tool.reefjournal.dto.ReefExportResponse;
import com.tiankong44.tool.reefjournal.dto.ReefLogDto;
import com.tiankong44.tool.reefjournal.dto.ReefLogRequest;
import com.tiankong44.tool.reefjournal.dto.ReefPageResponse;
import com.tiankong44.tool.reefjournal.dto.ReefPhotoDto;
import com.tiankong44.tool.reefjournal.dto.ReefPhotoSaveRequest;
import com.tiankong44.tool.reefjournal.dto.ReefReminderSettingsDto;
import com.tiankong44.tool.reefjournal.dto.ReefTankDto;
import com.tiankong44.tool.reefjournal.dto.ReefTankSaveRequest;
import com.tiankong44.tool.reefjournal.dto.ReefTrendResponse;
import com.tiankong44.tool.reefjournal.dto.ReefWaterRangeRequest;
import com.tiankong44.tool.reefjournal.dto.ReefLoginRequest;
import com.tiankong44.tool.reefjournal.dto.ReefLoginResponse;
import com.tiankong44.tool.reefjournal.dto.ReefSubscriptionConsentRequest;
import com.tiankong44.tool.reefjournal.service.ReefAuthService;
import com.tiankong44.tool.reefjournal.service.ReefBiologyService;
import com.tiankong44.tool.reefjournal.service.ReefEquipmentService;
import com.tiankong44.tool.reefjournal.service.ReefExportService;
import com.tiankong44.tool.reefjournal.service.ReefLogService;
import com.tiankong44.tool.reefjournal.service.ReefPhotoService;
import com.tiankong44.tool.reefjournal.service.ReefReminderService;
import com.tiankong44.tool.reefjournal.service.ReefTankService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 海缸日记小程序接口控制器，只负责协议接入和响应返回。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@RestController
@Validated
@RequestMapping("/reef-journal")
public class ReefJournalController {
    private final ReefAuthService authService;
    private final ReefTankService tankService;
    private final ReefBiologyService biologyService;
    private final ReefLogService logService;
    private final ReefEquipmentService equipmentService;
    private final ReefPhotoService photoService;
    private final ReefReminderService reminderService;
    private final ReefExportService exportService;
    private final ReefPhotoTokenService photoTokenService;

    /**
     * 创建只负责接口协议编排的海缸控制器。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param authService 微信登录服务
     * @param tankService 鱼缸服务
     * @param biologyService 生物服务
     * @param logService 日志及水质服务
     * @param equipmentService 设备服务
     * @param photoService 照片服务
     * @param reminderService 提醒服务
     * @param exportService JSON 导出服务
     */
    public ReefJournalController(ReefAuthService authService, ReefTankService tankService, ReefBiologyService biologyService,
                                 ReefLogService logService, ReefEquipmentService equipmentService, ReefPhotoService photoService,
                                 ReefReminderService reminderService, ReefExportService exportService,
                                 ReefPhotoTokenService photoTokenService) {
        this.authService = authService;
        this.tankService = tankService;
        this.biologyService = biologyService;
        this.logService = logService;
        this.equipmentService = equipmentService;
        this.photoService = photoService;
        this.reminderService = reminderService;
        this.exportService = exportService;
        this.photoTokenService = photoTokenService;
    }

    /**
     * 使用小程序一次性登录凭证建立模块登录态。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param request 登录凭证 DTO
     * @return 登录令牌和字符串用户编号
     */
    @PostMapping("/auth/login")
    public BaseRes<ReefLoginResponse> login(@RequestBody @Valid ReefLoginRequest request) {
        // 通过认证服务交换微信凭证并签发模块访问令牌。
        return BaseRes.success(authService.login(request.getCode()));
    }

    /**
     * 获取当前用户首页鱼缸摘要。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 拦截器写入的已认证用户编号
     * @return 当前用户首页数据
     */
    @GetMapping("/dashboard")
    public BaseRes<ReefDashboardResponse> dashboard(@RequestAttribute(ReefJournalConstants.REQUEST_USER_ID) Long userId) {
        // 读取用户自己的鱼缸摘要和最近日志。
        return BaseRes.success(tankService.dashboard(userId));
    }

    /**
     * 分页读取当前用户鱼缸。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 已认证用户编号
     * @param pageNum 页码
     * @param pageSize 单页条数
     * @return 当前用户鱼缸分页
     */
    @GetMapping("/tanks")
    public BaseRes<ReefPageResponse<ReefTankDto>> listTanks(@RequestAttribute(ReefJournalConstants.REQUEST_USER_ID) Long userId,
                                                            @RequestParam(required = false) Integer pageNum,
                                                            @RequestParam(required = false) Integer pageSize) {
        // 通过服务端用户编号过滤鱼缸并统一分页。
        return BaseRes.success(tankService.list(userId, pageNum, pageSize));
    }

    /**
     * 新增或更新鱼缸资料。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 已认证用户编号
     * @param request 鱼缸资料 DTO
     * @return 保存后的鱼缸
     */
    @PostMapping("/tanks")
    public BaseRes<ReefTankDto> saveTank(@RequestAttribute(ReefJournalConstants.REQUEST_USER_ID) Long userId,
                                        @RequestBody @Valid ReefTankSaveRequest request) {
        // 按用户归属保存鱼缸资料。
        return BaseRes.success(tankService.save(userId, request));
    }

    /**
     * 读取当前用户鱼缸详情。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 已认证用户编号
     * @param tankId 鱼缸编号
     * @return 鱼缸详情
     */
    @GetMapping("/tanks/{tankId}")
    public BaseRes<ReefTankDto> getTank(@RequestAttribute(ReefJournalConstants.REQUEST_USER_ID) Long userId,
                                       @PathVariable String tankId) {
        // 读取当前用户拥有的鱼缸。
        return BaseRes.success(tankService.get(userId, tankService.parseId(tankId)));
    }

    /**
     * 保存单个鱼缸的水质范围。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 已认证用户编号
     * @param tankId 鱼缸编号
     * @param request 水质范围 DTO
     * @return 更新后的鱼缸详情
     */
    @PutMapping("/tanks/{tankId}/water-ranges")
    public BaseRes<ReefTankDto> saveWaterRanges(@RequestAttribute(ReefJournalConstants.REQUEST_USER_ID) Long userId,
                                               @PathVariable String tankId,
                                               @RequestBody @Valid ReefWaterRangeRequest request) {
        // 校验范围属于用户指定鱼缸后保存。
        Long id = tankService.parseId(tankId);
        tankService.saveWaterRanges(userId, id, request);
        return BaseRes.success(tankService.get(userId, id));
    }

    /**
     * 按鱼缸分页读取生物。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 已认证用户编号
     * @param tankId 鱼缸编号
     * @param pageNum 页码
     * @param pageSize 单页条数
     * @return 当前鱼缸生物分页
     */
    @GetMapping("/biology")
    public BaseRes<ReefPageResponse<ReefBiologyDto>> listBiology(@RequestAttribute(ReefJournalConstants.REQUEST_USER_ID) Long userId,
                                                                 @RequestParam String tankId,
                                                                 @RequestParam(required = false) Integer pageNum,
                                                                 @RequestParam(required = false) Integer pageSize) {
        // 使用服务端用户编号和鱼缸归属过滤生物。
        return BaseRes.success(biologyService.list(userId, tankService.parseId(tankId), pageNum, pageSize));
    }

    /**
     * 新增或更新生物资料。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 已认证用户编号
     * @param request 生物资料 DTO
     * @return 保存后的生物
     */
    @PostMapping("/biology")
    public BaseRes<ReefBiologyDto> saveBiology(@RequestAttribute(ReefJournalConstants.REQUEST_USER_ID) Long userId,
                                              @RequestBody @Valid ReefBiologySaveRequest request) {
        // 保存生物并维护入缸或状态变更事件。
        return BaseRes.success(biologyService.save(userId, request));
    }

    /**
     * 读取当前用户生物详情。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 已认证用户编号
     * @param biologyId 生物编号
     * @return 生物详情
     */
    @GetMapping("/biology/{biologyId}")
    public BaseRes<ReefBiologyDto> getBiology(@RequestAttribute(ReefJournalConstants.REQUEST_USER_ID) Long userId,
                                              @PathVariable String biologyId) {
        // 通过用户归属读取生物。
        return BaseRes.success(biologyService.get(userId, tankService.parseId(biologyId)));
    }

    /**
     * 读取当前用户生物事件时间轴。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 已认证用户编号
     * @param biologyId 生物编号
     * @return 事件集合，无记录时为空数组
     */
    @GetMapping("/biology/{biologyId}/events")
    public BaseRes<List<ReefBiologyEventDto>> listBiologyEvents(@RequestAttribute(ReefJournalConstants.REQUEST_USER_ID) Long userId,
                                                                @PathVariable String biologyId) {
        // 读取指定用户生物的事件时间轴。
        return BaseRes.success(biologyService.events(userId, tankService.parseId(biologyId)));
    }

    /**
     * 新增生物事件。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 已认证用户编号
     * @param biologyId 生物编号
     * @param request 事件 DTO
     * @return 事件时间轴
     */
    @PostMapping("/biology/{biologyId}/events")
    public BaseRes<List<ReefBiologyEventDto>> saveBiologyEvent(@RequestAttribute(ReefJournalConstants.REQUEST_USER_ID) Long userId,
                                                               @PathVariable String biologyId,
                                                               @RequestBody @Valid ReefBiologyEventSaveRequest request) {
        // 将用户记录的事件追加到指定生物时间轴。
        return BaseRes.success(biologyService.saveEvent(userId, tankService.parseId(biologyId), request));
    }

    /**
     * 按类型和鱼缸分页读取日志。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 已认证用户编号
     * @param type 可选日志类型
     * @param tankId 可选鱼缸编号
     * @param pageNum 页码
     * @param pageSize 单页条数
     * @return 日志分页
     */
    @GetMapping("/logs")
    public BaseRes<ReefPageResponse<ReefLogDto>> listLogs(@RequestAttribute(ReefJournalConstants.REQUEST_USER_ID) Long userId,
                                                          @RequestParam(required = false) String type,
                                                          @RequestParam(required = false) String tankId,
                                                          @RequestParam(required = false) Integer pageNum,
                                                          @RequestParam(required = false) Integer pageSize) {
        // 日志过滤条件由服务以参数化查询执行。
        return BaseRes.success(logService.list(userId, type, tankId, pageNum, pageSize));
    }

    /**
     * 新增或更新海缸日志。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 已认证用户编号
     * @param request 日志 DTO
     * @return 保存后的日志详情
     */
    @PostMapping("/logs")
    public BaseRes<ReefLogDto> saveLog(@RequestAttribute(ReefJournalConstants.REQUEST_USER_ID) Long userId,
                                       @RequestBody @Valid ReefLogRequest request) {
        // 保存日志前校验关联鱼缸和生物的用户归属。
        return BaseRes.success(logService.save(userId, request));
    }

    /**
     * 读取当前用户的单条日志详情。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 已认证用户编号
     * @param logId 日志字符串编号
     * @return 日志详情和关联生物编号
     */
    @GetMapping("/logs/{logId}")
    public BaseRes<ReefLogDto> getLog(@RequestAttribute(ReefJournalConstants.REQUEST_USER_ID) Long userId,
                                    @PathVariable String logId) {
        // 通过服务端用户编号读取日志详情。
        return BaseRes.success(logService.get(userId, tankService.parseId(logId)));
    }

    /**
     * 删除当前用户的一条日志。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 已认证用户编号
     * @param logId 日志字符串编号
     * @return 空成功响应
     */
    @DeleteMapping("/logs/{logId}")
    public BaseRes<Void> deleteLog(@RequestAttribute(ReefJournalConstants.REQUEST_USER_ID) Long userId,
                                   @PathVariable String logId) {
        // 服务会核验归属并阻止删除仍被照片引用的日志。
        logService.delete(userId, tankService.parseId(logId));
        return BaseRes.success();
    }

    /**
     * 查询指定鱼缸水质趋势与用户设置范围产生的提醒。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 已认证用户编号
     * @param tankId 鱼缸编号
     * @param metric 水质参数编码
     * @param days 统计天数，零表示全部
     * @return 趋势统计、采样点及异常提醒
     */
    @GetMapping("/water/trend")
    public BaseRes<ReefTrendResponse> waterTrend(@RequestAttribute(ReefJournalConstants.REQUEST_USER_ID) Long userId,
                                                @RequestParam String tankId,
                                                @RequestParam String metric,
                                                @RequestParam(required = false) Integer days) {
        // 读取指定鱼缸趋势并应用其用户自设范围。
        return BaseRes.success(logService.trend(userId, tankService.parseId(tankId), metric, days));
    }

    /**
     * 分页读取用户设备。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 已认证用户编号
     * @param category 设备分类
     * @param pageNum 页码
     * @param pageSize 单页条数
     * @return 设备分页
     */
    @GetMapping("/equipment")
    public BaseRes<ReefPageResponse<ReefEquipmentDto>> listEquipment(@RequestAttribute(ReefJournalConstants.REQUEST_USER_ID) Long userId,
                                                                     @RequestParam(required = false) String category,
                                                                     @RequestParam(required = false) String tankId,
                                                                     @RequestParam(required = false) Integer pageNum,
                                                                     @RequestParam(required = false) Integer pageSize) {
        // 只读取当前用户的设备资料。
        return BaseRes.success(equipmentService.list(userId, category, tankId, pageNum, pageSize));
    }

    /**
     * 新增或更新用户设备资料。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 已认证用户编号
     * @param request 设备 DTO
     * @return 保存后的设备详情
     */
    @PostMapping("/equipment")
    public BaseRes<ReefEquipmentDto> saveEquipment(@RequestAttribute(ReefJournalConstants.REQUEST_USER_ID) Long userId,
                                                  @RequestBody @Valid ReefEquipmentSaveRequest request) {
        // 保存设备和用户维护周期。
        return BaseRes.success(equipmentService.save(userId, request));
    }

    /**
     * 读取当前用户的单台设备资料。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 已认证用户编号
     * @param equipmentId 设备字符串编号
     * @return 设备详情
     */
    @GetMapping("/equipment/{equipmentId}")
    public BaseRes<ReefEquipmentDto> getEquipment(@RequestAttribute(ReefJournalConstants.REQUEST_USER_ID) Long userId,
                                                  @PathVariable String equipmentId) {
        // 按当前用户编号读取设备，避免跨账号访问。
        return BaseRes.success(equipmentService.get(userId, tankService.parseId(equipmentId)));
    }

    /**
     * 分页读取当前用户影像记录。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 已认证用户编号
     * @param category 可选照片分类
     * @param pageNum 页码
     * @param pageSize 单页条数
     * @return 照片记录分页
     */
    @GetMapping("/photos")
    public BaseRes<ReefPageResponse<ReefPhotoDto>> listPhotos(@RequestAttribute(ReefJournalConstants.REQUEST_USER_ID) Long userId,
                                                              @RequestParam(required = false) String category,
                                                              @RequestParam(required = false) String referenceType,
                                                              @RequestParam(required = false) String referenceId,
                                                              @RequestParam(required = false) Integer pageNum,
                                                              @RequestParam(required = false) Integer pageSize) {
        // 读取当前用户照片元数据和受保护内容链接。
        return BaseRes.success(photoService.list(userId, category, referenceType, referenceId, pageNum, pageSize));
    }

    /**
     * 将小程序上传的图片保存到共享云端存储并登记归属。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 已认证用户编号
     * @param file 图片文件
     * @param title 照片标题
     * @param category 照片分类
     * @param referenceType 可选关联类型
     * @param referenceId 可选关联编号
     * @param recordedAt 可选上海本地记录时间
     * @param note 可选照片备注
     * @return 照片元数据
     */
    @PostMapping(value = "/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseRes<ReefPhotoDto> uploadPhoto(@RequestAttribute(ReefJournalConstants.REQUEST_USER_ID) Long userId,
                                             @RequestParam("file") MultipartFile file,
                                             @RequestParam(required = false) String title,
                                             @RequestParam String category,
                                             @RequestParam(required = false) String referenceType,
                                             @RequestParam(required = false) String referenceId,
                                             @RequestParam(required = false) String recordedAt,
                                             @RequestParam(required = false) String note) {
        // 复用公共云端文件服务，并将照片关联到当前用户。
        return BaseRes.success(photoService.upload(userId, file, title, category, referenceType, referenceId, recordedAt, note));
    }

    /**
     * 读取当前用户的单张照片详情和签名内容地址。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 已认证用户编号
     * @param photoId 照片字符串编号
     * @return 当前用户的照片资料
     */
    @GetMapping("/photos/{photoId}")
    public BaseRes<ReefPhotoDto> getPhoto(@RequestAttribute(ReefJournalConstants.REQUEST_USER_ID) Long userId,
                                        @PathVariable String photoId) {
        // 读取照片前由照片服务检查用户归属。
        return BaseRes.success(photoService.get(userId, tankService.parseId(photoId)));
    }

    /**
     * 更新当前用户照片的分类、关联对象、记录时间和备注。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 已认证用户编号
     * @param photoId 照片字符串编号
     * @param request 影像记录修改 DTO
     * @return 保存后的照片详情
     */
    @PutMapping("/photos/{photoId}")
    public BaseRes<ReefPhotoDto> updatePhoto(@RequestAttribute(ReefJournalConstants.REQUEST_USER_ID) Long userId,
                                             @PathVariable String photoId,
                                             @RequestBody @Valid ReefPhotoSaveRequest request) {
        // 更新前由照片服务校验分类、时间和关联对象归属。
        return BaseRes.success(photoService.update(userId, tankService.parseId(photoId), request));
    }

    /**
     * 删除当前用户的照片记录及其云端文件内容。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 已认证用户编号
     * @param photoId 照片字符串编号
     * @return 空成功响应
     */
    @DeleteMapping("/photos/{photoId}")
    public BaseRes<Void> deletePhoto(@RequestAttribute(ReefJournalConstants.REQUEST_USER_ID) Long userId,
                                     @PathVariable String photoId) {
        // 删除照片前由照片服务校验归属并清理云端文件。
        photoService.delete(userId, tankService.parseId(photoId));
        return BaseRes.success();
    }

    /**
     * 在确认照片归属后读取云端图片内容。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 已认证用户编号
     * @param photoId 照片编号
     * @return 图片字节流
     */
    @GetMapping("/photos/{photoId}/content")
    public ResponseEntity<byte[]> photoContent(@PathVariable String photoId, @RequestParam String signature) {
        Long userId = photoTokenService.verify(photoId, signature);
        if (userId == null) return ResponseEntity.status(401).build();
        // 先通过照片服务核验用户归属，再读取共享存储文件。
        CloudFile file = photoService.content(userId, tankService.parseId(photoId));
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (file.getContentType() != null) {
            try { mediaType = MediaType.parseMediaType(file.getContentType()); }
            catch (IllegalArgumentException exception) { mediaType = MediaType.APPLICATION_OCTET_STREAM; }
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentLength(file.getFileSize() == null ? 0L : file.getFileSize());
        headers.setContentDisposition(ContentDisposition.inline().filename(file.getFileKey(), StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(file.getFileContent());
    }

    /**
     * 查询当前用户提醒偏好和授权额度。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 已认证用户编号
     * @return 提醒设置
     */
    @GetMapping("/reminders")
    public BaseRes<ReefReminderSettingsDto> getReminders(@RequestAttribute(ReefJournalConstants.REQUEST_USER_ID) Long userId) {
        // 读取并初始化当前用户提醒选项。
        return BaseRes.success(reminderService.get(userId));
    }

    /**
     * 保存当前用户提醒时间和开关。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 已认证用户编号
     * @param settings 提醒设置 DTO
     * @return 更新后的提醒设置
     */
    @PutMapping("/reminders")
    public BaseRes<ReefReminderSettingsDto> saveReminders(@RequestAttribute(ReefJournalConstants.REQUEST_USER_ID) Long userId,
                                                          @RequestBody @Valid ReefReminderSettingsDto settings) {
        // 保存提醒偏好后返回服务端确认状态。
        reminderService.save(userId, settings);
        return BaseRes.success(reminderService.get(userId));
    }

    /**
     * 记录小程序订阅弹窗返回的授权额度结果。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 已认证用户编号
     * @param request 授权结果 DTO
     * @return 空成功响应
     */
    @PostMapping("/reminders/consent")
    public BaseRes<Void> recordConsent(@RequestAttribute(ReefJournalConstants.REQUEST_USER_ID) Long userId,
                                       @RequestBody @Valid ReefSubscriptionConsentRequest request) {
        // 仅为当前用户登记本次明确授权结果。
        reminderService.recordConsent(userId, request);
        return BaseRes.success();
    }

    /**
     * 导出当前用户 JSON 备份资料。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 已认证用户编号
     * @return 当前用户的结构化备份内容
     */
    @GetMapping("/export")
    public BaseRes<ReefExportResponse> export(@RequestAttribute(ReefJournalConstants.REQUEST_USER_ID) Long userId) {
        // 只组装当前登录用户拥有的海缸业务数据。
        return BaseRes.success(exportService.export(userId));
    }
}
