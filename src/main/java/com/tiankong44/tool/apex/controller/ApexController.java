package com.tiankong44.tool.apex.controller;

import com.tiankong44.tool.apex.dto.BackfillRequest;
import com.tiankong44.tool.apex.dto.OpenCountRequest;
import com.tiankong44.tool.apex.dto.UserCreateRequest;
import com.tiankong44.tool.apex.service.ApexService;
import com.tiankong44.tool.base.entity.BaseRes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.time.LocalDate;

/**
 * Apex用户统计接口控制器
 */
@RestController
@RequestMapping("/apex")
@Validated
public class ApexController {
    @Autowired
    private ApexService apexService;

    /**
     * 获取用户统计数据
     * URL: /apex/users/{userId}/stats
     * Method: GET
     */
    @GetMapping("/users/{userId}/stats")
    public BaseRes getUserStats(@PathVariable @NotBlank String userId) {
        return apexService.getUserStats(userId);
    }

    /**
     * 获取用户每日记录
     * URL: /apex/users/{userId}/daily-records
     * Method: GET
     */
    @GetMapping("/users/{userId}/daily-records")
    public BaseRes getUserDailyRecords(
            @PathVariable @NotBlank String userId,
            @RequestParam(required = false) @Min(1) Integer limit,
            @RequestParam(required = false) @Min(0) Integer offset) {
        return apexService.getUserDailyRecords(userId, limit, offset);
    }

    /**
     * 增加今日开启数量
     * URL: /apex/users/{userId}/open
     * Method: POST
     */
    @PostMapping("/users/{userId}/open")
    public BaseRes addTodayCount(
            @PathVariable @NotBlank String userId,
            @RequestBody @Valid OpenCountRequest request) {
        return apexService.addTodayCount(userId, request.getCount());
    }

    /**
     * 补录历史记录
     * URL: /apex/users/{userId}/backfill
     * Method: POST
     */
    @PostMapping("/users/{userId}/backfill")
    public BaseRes backfillHistoryRecord(
            @PathVariable @NotBlank String userId,
            @RequestBody @Valid BackfillRequest request) {
        return apexService.backfillHistoryRecord(userId, request.getDate(), request.getCount());
    }

    /**
     * 获取指定日期记录
     * URL: /apex/users/{userId}/daily-records/{date}
     * Method: GET
     */
    @GetMapping("/users/{userId}/daily-records/{date}")
    public BaseRes getDailyRecordByDate(
            @PathVariable @NotBlank String userId,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return apexService.getDailyRecordByDate(userId, date);
    }

    /**
     * 检查用户是否存在
     * URL: /apex/users/{userId}
     * Method: GET
     */
    @GetMapping("/users/{userId}")
    public BaseRes checkUserExists(@PathVariable @NotBlank String userId) {
        return apexService.checkUserExists(userId);
    }

    /**
     * 创建新用户
     * URL: /apex/users
     * Method: POST
     */
    @PostMapping("/users")
    public BaseRes createUser(@RequestBody @Valid UserCreateRequest request) {
        return apexService.createUser(request.getUserId());
    }
}