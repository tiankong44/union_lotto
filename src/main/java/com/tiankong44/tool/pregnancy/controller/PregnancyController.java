package com.tiankong44.tool.pregnancy.controller;

import com.tiankong44.tool.base.entity.BaseRes;
import com.tiankong44.tool.pregnancy.dto.AntenatalTaskSaveRequest;
import com.tiankong44.tool.pregnancy.dto.ContractionSaveRequest;
import com.tiankong44.tool.pregnancy.dto.FetalMovementSessionSaveRequest;
import com.tiankong44.tool.pregnancy.dto.HealthRecordSaveRequest;
import com.tiankong44.tool.pregnancy.dto.ProfileSaveRequest;
import com.tiankong44.tool.pregnancy.dto.RecordDeleteRequest;
import com.tiankong44.tool.pregnancy.dto.RecordNoteUpdateRequest;
import com.tiankong44.tool.pregnancy.service.PregnancyService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.time.LocalDate;

/**
 * 孕期助手接口控制器，面向单用户记录工具提供统一接口。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-08-06
 */
@RestController
@Validated
@RequestMapping("/pregnancy")
public class PregnancyController {
    @Resource
    private PregnancyService pregnancyService;

    /**
     * 查询孕期档案。
     *
     * @return 孕期档案
     */
    @GetMapping("/profile")
    public BaseRes getProfile() {
        // 调用档案查询服务，为前端恢复本地和服务端状态提供基础资料。
        return pregnancyService.getProfile();
    }

    /**
     * 保存孕期档案。
     *
     * @param request 档案请求
     * @return 保存结果
     */
    @PutMapping("/profile")
    public BaseRes saveProfile(@RequestBody @Valid ProfileSaveRequest request) {
        // 调用档案保存服务，按单用户唯一更新档案。
        return pregnancyService.saveProfile(request);
    }

    /**
     * 查询胎动会话历史。
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 胎动会话列表
     */
    @GetMapping("/fetal-movement/sessions")
    public BaseRes listFetalMovements(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        // 调用胎动历史查询服务，为历史页和趋势页提供数据。
        return pregnancyService.listFetalMovements(startDate, endDate);
    }

    /**
     * 保存胎动会话。
     *
     * @param request 胎动会话请求
     * @return 保存结果
     */
    @PostMapping("/fetal-movement/sessions")
    public BaseRes saveFetalMovement(@RequestBody @Valid FetalMovementSessionSaveRequest request) {
        // 调用胎动会话保存服务，使用客户端编号保障重复提交幂等。
        return pregnancyService.saveFetalMovement(request);
    }

    /**
     * 查询宫缩记录。
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 宫缩列表
     */
    @GetMapping("/contractions")
    public BaseRes listContractions(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        // 调用宫缩历史查询服务，为计时页和历史页提供数据。
        return pregnancyService.listContractions(startDate, endDate);
    }

    /**
     * 保存宫缩记录。
     *
     * @param request 宫缩请求
     * @return 保存结果
     */
    @PostMapping("/contractions")
    public BaseRes saveContraction(@RequestBody @Valid ContractionSaveRequest request) {
        // 调用宫缩保存服务，使用客户端编号保障重复提交幂等。
        return pregnancyService.saveContraction(request);
    }

    /**
     * 查询孕期健康记录。
     *
     * @param recordType 记录类型
     * @return 健康记录列表
     */
    @GetMapping("/health-records")
    public BaseRes listHealthRecords(@RequestParam(required = false) String recordType) {
        // 调用健康记录查询服务，为健康页和统计页提供数据。
        return pregnancyService.listHealthRecords(recordType);
    }

    /**
     * 保存孕期健康记录。
     *
     * @param request 健康记录请求
     * @return 保存结果
     */
    @PostMapping("/health-records")
    public BaseRes saveHealthRecord(@RequestBody @Valid HealthRecordSaveRequest request) {
        // 调用健康记录保存服务，只保存用户自述数据，不做医疗判断。
        return pregnancyService.saveHealthRecord(request);
    }

    /**
     * 查询产检待办。
     *
     * @param status 待办状态
     * @return 待办列表
     */
    @GetMapping("/tasks")
    public BaseRes listTasks(@RequestParam(required = false) String status) {
        // 调用待办查询服务，为时间线和提醒页提供数据。
        return pregnancyService.listTasks(status);
    }

    /**
     * 保存产检待办。
     *
     * @param request 待办请求
     * @return 保存结果
     */
    @PostMapping("/tasks")
    public BaseRes saveTask(@RequestBody @Valid AntenatalTaskSaveRequest request) {
        // 调用待办保存服务，使用客户端编号保障重复提交幂等。
        return pregnancyService.saveTask(request);
    }

    /**
     * 更新孕期记录备注。
     *
     * @param request 记录类型、客户端编号和备注内容
     * @return 更新后的记录或业务失败信息
     */
    @PutMapping("/record-notes")
    public BaseRes updateRecordNote(@RequestBody @Valid RecordNoteUpdateRequest request) {
        // 调用记录备注更新服务，统一处理四类记录的补充、修改和清空。
        return pregnancyService.updateRecordNote(request);
    }

    /**
     * 删除单条孕期历史记录。
     *
     * @param request 记录类型和客户端记录编号
     * @return 删除结果；记录不存在、类型或状态不支持时返回业务失败
     */
    @DeleteMapping("/records")
    public BaseRes deleteRecord(@RequestBody @Valid RecordDeleteRequest request) {
        // 调用记录删除服务，按记录类型清理目标记录及其从属数据。
        return pregnancyService.deleteRecord(request);
    }

    /**
     * 查询个人记录汇总。
     *
     * @return 汇总指标
     */
    @GetMapping("/summary")
    public BaseRes getSummary() {
        // 调用个人汇总服务，只对用户自身历史记录做描述性统计。
        return pregnancyService.getSummary();
    }

}
