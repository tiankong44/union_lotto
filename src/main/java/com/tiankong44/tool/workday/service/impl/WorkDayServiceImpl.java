package com.tiankong44.tool.workday.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tiankong44.tool.base.entity.BaseRes;
import com.tiankong44.tool.mapper.FutureWorkDayMapper;
import com.tiankong44.tool.mapper.HisWorkDayMapper;
import com.tiankong44.tool.mapper.LottoMapper;
import com.tiankong44.tool.mapper.WorkdayConfigMapper;
import com.tiankong44.tool.workday.entity.FutureWorkDay;
import com.tiankong44.tool.workday.entity.HisWorkDay;
import com.tiankong44.tool.workday.entity.config.WorkdayConfig;
import com.tiankong44.tool.workday.service.WorkDayService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description :
 * @Author zhanghao_SMEICS
 * @Date 2025/5/27  10:42
 **/
@Service
public class WorkDayServiceImpl implements WorkDayService {

    @Resource
    HisWorkDayMapper hisWorkDayMapper;
    @Resource
    FutureWorkDayMapper futureWorkDayMapper;
    @Resource
    WorkdayConfigMapper workdayConfigMapper;

    @Override
    public BaseRes getSchedule() {
        // 获取当前日期
        LocalDate now = LocalDate.now();
        // 计算前6个月的第一天
        LocalDate startDate = now.minusMonths(6).withDayOfMonth(1);
        // 计算后6个月的最后一天
        LocalDate endDate = now.plusMonths(6).withDayOfMonth(now.plusMonths(6).getMonth().length(now.isLeapYear()));
        List<HisWorkDay> hisWorkDays = hisWorkDayMapper.selectList(new LambdaQueryWrapper<HisWorkDay>().between(HisWorkDay::getWorkDate, startDate, now));

        List<FutureWorkDay> futureWorkDays = futureWorkDayMapper.selectList(new LambdaQueryWrapper<FutureWorkDay>().between(FutureWorkDay::getWorkDate, now, endDate));
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("hisWorkDays", hisWorkDays);
        jsonObject.put("futureWorkDays", futureWorkDays);
        return BaseRes.success(jsonObject);
    }

    @Override
    public BaseRes getConfig() {
        List<WorkdayConfig> workdayConfigs = new ArrayList<>();
        workdayConfigs = workdayConfigMapper.selectList(new LambdaQueryWrapper<WorkdayConfig>().eq(WorkdayConfig::getStatus, 1));

        return BaseRes.success(workdayConfigs);
    }
}
