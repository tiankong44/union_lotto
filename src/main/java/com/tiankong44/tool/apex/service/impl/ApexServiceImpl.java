package com.tiankong44.tool.apex.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tiankong44.tool.apex.entity.ApexUser;
import com.tiankong44.tool.apex.entity.ApexUserDailyRecord;
import com.tiankong44.tool.apex.mapper.ApexUserDailyRecordMapper;
import com.tiankong44.tool.apex.mapper.ApexUserMapper;
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
    private ApexUserDailyRecordMapper apexUserDailyRecordMapper;

    @Override
    public BaseRes getUserStats(String userId) {
        // 检查用户是否存在
        if (!doesUserExist(userId)) {
            return BaseRes.failure("用户不存在");
        }

        // 查询用户创建时间
        QueryWrapper<ApexUser> userQuery = new QueryWrapper<>();
        userQuery.eq("user_id", userId);
        ApexUser user = apexUserMapper.selectOne(userQuery);

        if (user == null) {
            return BaseRes.failure("无法获取用户信息");
        }

        // 统计用户的总开启数量
        QueryWrapper<ApexUserDailyRecord> recordQuery = new QueryWrapper<>();
        recordQuery.eq("user_id", userId);
        List<ApexUserDailyRecord> records = apexUserDailyRecordMapper.selectList(recordQuery);

        int totalOpened = 0;
        if (records != null) {
            for (ApexUserDailyRecord record : records) {
                totalOpened += record.getCount();
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("totalOpened", totalOpened);
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

        // 查询用户每日记录，按ID倒序排列
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


        ApexUserDailyRecord todayRecord = new ApexUserDailyRecord();
        todayRecord.setUserId(userId);
        todayRecord.setRecordDate(today);
        todayRecord.setCount(count);
        apexUserDailyRecordMapper.insert(todayRecord);


        // 重新统计总开启数量
        QueryWrapper<ApexUserDailyRecord> recordQuery = new QueryWrapper<>();
        recordQuery.eq("user_id", userId);
        List<ApexUserDailyRecord> records = apexUserDailyRecordMapper.selectList(recordQuery);

        int totalOpened = 0;
        int todayCount = 0;
        if (records != null) {
            for (ApexUserDailyRecord record : records) {
                if (record.getRecordDate().equals(today)) {
                    todayCount += record.getCount();
                }
                totalOpened += record.getCount();
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalOpened", totalOpened);
        result.put("todayCount", todayCount);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("message", "Successfully added " + count + " boxes for today");
        responseData.put("data", result);

        return BaseRes.success(responseData);
    }


    @Override
    @Transactional
    public BaseRes deleteRecord(String userId, Integer id) {
        // 检查用户是否存在
        if (!doesUserExist(userId)) {
            return BaseRes.failure("用户不存在");
        }

        // 检查ID参数是否有效
        if (id == null) {
            return BaseRes.failure("记录ID不能为空");
        }

        // 检查记录是否存在且属于该用户
        ApexUserDailyRecord record = apexUserDailyRecordMapper.selectById(id);
        if (record == null) {
            return BaseRes.failure("记录不存在");
        }

        // 验证该记录是否属于指定用户
        if (!userId.equals(record.getUserId())) {
            return BaseRes.failure("无权限删除该记录");
        }

        // 执行删除操作
        int deletedRows = apexUserDailyRecordMapper.deleteById(id);
        if (deletedRows > 0) {
            // 重新统计总开启数量
            QueryWrapper<ApexUserDailyRecord> recordQuery = new QueryWrapper<>();
            recordQuery.eq("user_id", userId);
            List<ApexUserDailyRecord> records = apexUserDailyRecordMapper.selectList(recordQuery);

            int totalOpened = 0;
            if (records != null) {
                for (ApexUserDailyRecord r : records) {
                    totalOpened += r.getCount();
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("totalOpened", totalOpened);
            result.put("deletedId", id);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("message", "记录删除成功");
            responseData.put("data", result);

            return BaseRes.success(responseData);
        } else {
            return BaseRes.failure("删除失败");
        }
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


        ApexUserDailyRecord record = new ApexUserDailyRecord();
        record.setUserId(userId);
        record.setRecordDate(date);
        record.setCount(count);
        apexUserDailyRecordMapper.insert(record);
        // 重新统计总开启数量
        QueryWrapper<ApexUserDailyRecord> recordQuery = new QueryWrapper<>();
        recordQuery.eq("user_id", userId);
        List<ApexUserDailyRecord> records = apexUserDailyRecordMapper.selectList(recordQuery);

        int totalOpened = 0;
        if (records != null) {
            for (ApexUserDailyRecord r : records) {
                totalOpened += r.getCount();
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalOpened", totalOpened);

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

        // 查询指定日期的记录，按ID倒序排列
        List<ApexUserDailyRecord> records = apexUserDailyRecordMapper.selectByUserIdAndDate(userId, date);

        int totalOpened = 0;
        if (records != null) {
            for (ApexUserDailyRecord r : records) {
                totalOpened += r.getCount();
            }
        }

        Map<String, Object> result = new HashMap<>();
        // 使用第一条记录（最新的）作为日期信息
        if (records != null && !records.isEmpty()) {
            result.put("date", records.get(0).getRecordDate());
            result.put("latestRecord", records.get(0)); // 添加最新的记录信息
        }
        result.put("count", totalOpened);
        result.put("recordCount", records != null ? records.size() : 0); // 添加记录总数

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