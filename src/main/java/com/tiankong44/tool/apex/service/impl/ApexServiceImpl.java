package com.tiankong44.tool.apex.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tiankong44.tool.apex.entity.ApexUser;
import com.tiankong44.tool.apex.entity.ApexUserDailyRecord;
import com.tiankong44.tool.apex.entity.ApexUserStats;
import com.tiankong44.tool.apex.mapper.ApexUserDailyRecordMapper;
import com.tiankong44.tool.apex.mapper.ApexUserMapper;
import com.tiankong44.tool.apex.mapper.ApexUserStatsMapper;
import com.tiankong44.tool.apex.service.ApexService;
import com.tiankong44.tool.base.entity.BaseRes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhanghao_SMEICS
 * @date 2022-10-21 21:46
 */
@Service
public class ApexServiceImpl implements ApexService {
    @Resource
    private ApexUserMapper apexUserMapper;

    @Resource
    private ApexUserStatsMapper apexUserStatsMapper;

    @Resource
    private ApexUserDailyRecordMapper apexUserDailyRecordMapper;

    @Override
    public BaseRes getUserStats(String userId) {
        // 检查用户是否存在
        if (!doesUserExist(userId)) {
            return BaseRes.failure("用户不存在");
        }

        // 查询用户统计信息
        QueryWrapper<ApexUserStats> statsQuery = new QueryWrapper<>();
        statsQuery.eq("user_id", userId);
        ApexUserStats userStats = apexUserStatsMapper.selectOne(statsQuery);

        if (userStats == null) {
            return BaseRes.failure("无法获取用户统计信息");
        }

        // 查询用户创建时间
        QueryWrapper<ApexUser> userQuery = new QueryWrapper<>();
        userQuery.eq("user_id", userId);
        ApexUser user = apexUserMapper.selectOne(userQuery);

        if (user == null) {
            return BaseRes.failure("无法获取用户信息");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("totalOpened", userStats.getTotalOpened());
        result.put("createdAt", user.getCreatedAt());

        return BaseRes.success(result);
    }

    @Override
    public BaseRes getUserDailyRecords(String userId, Integer limit, Integer offset) {
        // 检查用户是否存在
        if (!doesUserExist(userId)) {
            return BaseRes.failure("用户不存在");
        }

        // 设置默认值
        if (limit == null || limit <= 0) {
            limit = 50;
        }
        if (offset == null || offset < 0) {
            offset = 0;
        }

        // 查询用户每日记录
        List<ApexUserDailyRecord> records = apexUserDailyRecordMapper.selectByUserIdWithLimit(userId, limit, offset);

        return BaseRes.success(records);
    }

    @Override
    @Transactional
    public BaseRes addTodayCount(String userId, Integer count) {
        // 检查用户是否存在
        if (!doesUserExist(userId)) {
            return BaseRes.failure("用户不存在");
        }

        // 验证count参数
        if (count == null || count <= 0) {
            return BaseRes.failure("数量无效");
        }

        LocalDate today = LocalDate.now();

        // 查询今天的记录
        ApexUserDailyRecord todayRecord = apexUserDailyRecordMapper.selectByUserIdAndDate(userId, today);

        if (todayRecord == null) {
            // 如果今天没有记录，则创建新记录
            todayRecord = new ApexUserDailyRecord();
            todayRecord.setUserId(userId);
            todayRecord.setRecordDate(today);
            todayRecord.setCount(count);
            apexUserDailyRecordMapper.insert(todayRecord);
        } else {
            // 如果今天已有记录，则更新记录
            todayRecord.setCount(todayRecord.getCount() + count);
            apexUserDailyRecordMapper.updateById(todayRecord);
        }

        // 更新总统计数
        int updated = apexUserStatsMapper.incrementTotalOpened(userId, count);
        if (updated == 0) {
            return BaseRes.failure("更新总统计数失败");
        }

        // 查询最新的总统计数
        QueryWrapper<ApexUserStats> statsQuery = new QueryWrapper<>();
        statsQuery.eq("user_id", userId);
        ApexUserStats userStats = apexUserStatsMapper.selectOne(statsQuery);

        Map<String, Object> result = new HashMap<>();
        result.put("totalOpened", userStats.getTotalOpened());
        result.put("todayCount", todayRecord.getCount());

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("message", "Successfully added " + count + " boxes for today");
        responseData.put("data", result);

        return BaseRes.success(responseData);
    }

    @Override
    @Transactional
    public BaseRes backfillHistoryRecord(String userId, LocalDate date, Integer count) {
        // 检查用户是否存在
        if (!doesUserExist(userId)) {
            return BaseRes.failure("用户不存在");
        }

        // 验证参数
        if (date == null) {
            return BaseRes.failure("日期格式无效");
        }
        if (count == null || count < 0) {
            return BaseRes.failure("数量无效");
        }

        // 查询指定日期的记录
        ApexUserDailyRecord record = apexUserDailyRecordMapper.selectByUserIdAndDate(userId, date);

        int difference = count;
        if (record == null) {
            // 如果记录不存在，则创建新记录
            record = new ApexUserDailyRecord();
            record.setUserId(userId);
            record.setRecordDate(date);
            record.setCount(count);
            apexUserDailyRecordMapper.insert(record);
        } else {
            // 如果记录已存在，则计算差值并更新记录
            difference = count - record.getCount();
            record.setCount(count);
            apexUserDailyRecordMapper.updateById(record);
        }

        // 更新总统计数
        if (difference != 0) {
            int updated = apexUserStatsMapper.incrementTotalOpened(userId, difference);
            if (updated == 0) {
                return BaseRes.failure("更新总统计数失败");
            }
        }

        // 查询最新的总统计数
        QueryWrapper<ApexUserStats> statsQuery = new QueryWrapper<>();
        statsQuery.eq("user_id", userId);
        ApexUserStats userStats = apexUserStatsMapper.selectOne(statsQuery);

        Map<String, Object> result = new HashMap<>();
        result.put("totalOpened", userStats.getTotalOpened());

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("message", "Successfully backfilled " + count + " boxes for " + date.toString());
        responseData.put("data", result);

        return BaseRes.success(responseData);
    }

    @Override
    public BaseRes getDailyRecordByDate(String userId, LocalDate date) {
        // 检查用户是否存在
        if (!doesUserExist(userId)) {
            return BaseRes.failure("用户不存在");
        }

        // 验证参数
        if (date == null) {
            return BaseRes.failure("日期格式无效");
        }

        // 查询指定日期的记录
        ApexUserDailyRecord record = apexUserDailyRecordMapper.selectByUserIdAndDate(userId, date);

        if (record == null) {
            // 如果记录不存在，返回空数据
            return BaseRes.success(null);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("date", record.getRecordDate());
        result.put("count", record.getCount());

        return BaseRes.success(result);
    }

    @Override
    public BaseRes checkUserExists(String userId) {
        Map<String, Boolean> result = new HashMap<>();
        result.put("exists", doesUserExist(userId));
        return BaseRes.success(result);
    }

    @Override
    @Transactional
    public BaseRes createUser(String userId) {
        // 检查用户是否已存在
        if (doesUserExist(userId)) {
            return BaseRes.failure("用户已存在");
        }

        // 创建用户
        ApexUser user = new ApexUser();
        user.setUserId(userId);
        user.setCreatedAt(java.time.LocalDateTime.now());
        user.setUpdatedAt(java.time.LocalDateTime.now());
        apexUserMapper.insert(user);

        // 创建用户统计记录
        ApexUserStats userStats = new ApexUserStats();
        userStats.setUserId(userId);
        userStats.setTotalOpened(0);
        userStats.setCreatedAt(java.time.LocalDateTime.now());
        userStats.setUpdatedAt(java.time.LocalDateTime.now());
        apexUserStatsMapper.insert(userStats);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("message", "User created successfully");
        responseData.put("data", result);

        return BaseRes.success(responseData);
    }

    @Override
    public boolean doesUserExist(String userId) {
        QueryWrapper<ApexUser> query = new QueryWrapper<>();
        query.eq("user_id", userId);
        return apexUserMapper.selectCount(query) > 0;
    }
}