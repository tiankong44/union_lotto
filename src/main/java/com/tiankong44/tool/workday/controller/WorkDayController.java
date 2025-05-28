package com.tiankong44.tool.workday.controller;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.tiankong44.tool.annotation.SkipLogging;
import com.tiankong44.tool.base.entity.BaseRes;
import com.tiankong44.tool.lotto.entity.Draw;
import com.tiankong44.tool.lotto.entity.Lotto;
import com.tiankong44.tool.lotto.service.LottoService;
import com.tiankong44.tool.util.JsonUtils;
import com.tiankong44.tool.workday.service.WorkDayService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @Description :
 * @Author zhanghao_SMEICS
 * @Date 2024/9/3  13:04
 **/
@RestController
@RequestMapping("workday")
public class WorkDayController {
    @Resource
    WorkDayService workDayService;

    @PostMapping("/schedule")
    @SkipLogging
    public BaseRes schedule() {

        return workDayService.getSchedule();

    }


}
