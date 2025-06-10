package com.tiankong44.tool.workday.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tiankong44.tool.base.entity.BaseRes;
import com.tiankong44.tool.lotto.entity.Lotto;

import java.time.LocalDate;

/**

 * @Description :
 * @Author zhanghao_SMEICS
 * @Date 2025/5/27  10:41
 **/
public interface WorkDayService  {
    BaseRes getSchedule();

    BaseRes getConfig();
}
