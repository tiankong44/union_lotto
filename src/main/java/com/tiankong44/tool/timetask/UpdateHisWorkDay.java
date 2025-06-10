package com.tiankong44.tool.timetask;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tiankong44.tool.mapper.FutureWorkDayMapper;
import com.tiankong44.tool.mapper.HisWorkDayMapper;
import com.tiankong44.tool.workday.entity.FutureWorkDay;
import com.tiankong44.tool.workday.entity.HisWorkDay;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description :
 * @Author zhanghao_SMEICS
 * @Date 2025/5/27  13:16
 **/

@Component
public class UpdateHisWorkDay {
    @Resource
    HisWorkDayMapper hisWorkDayMapper;
    @Resource
    FutureWorkDayMapper futureWorkDayMapper;


    @Scheduled(cron = "0 55 23 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void updateCheckAlertPic() {
        String now = DateUtil.today();
        FutureWorkDay futureWorkDay = futureWorkDayMapper.selectOne(new LambdaQueryWrapper<FutureWorkDay>().eq(FutureWorkDay::getWorkDate, now).eq(FutureWorkDay::getStatus, "1"));
        HisWorkDay hisWorkDay = hisWorkDayMapper.selectOne(new LambdaQueryWrapper<HisWorkDay>().eq(HisWorkDay::getWorkDate, now).eq(HisWorkDay::getStatus, "1"));

        if (hisWorkDay == null) {
            hisWorkDay=new HisWorkDay();
            hisWorkDay.setWorkStatus(futureWorkDay.getWorkStatus());
            hisWorkDay.setWorkDate(futureWorkDay.getWorkDate());
            //hisWorkDay.setWorkTime(futureWorkDay.getWorkTime());
            //hisWorkDay.setWorkStatusDesc(futureWorkDay.getWorkStatusDesc());
            hisWorkDay.setStatus(1);
            //hisWorkDay.setIcon(futureWorkDay.getIcon());
            //hisWorkDay.setColor(futureWorkDay.getColor());

            hisWorkDayMapper.insert(hisWorkDay);
        }
    }
}
