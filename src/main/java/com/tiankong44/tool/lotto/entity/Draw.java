package com.tiankong44.tool.lotto.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Description :
 * @Author zhanghao_SMEICS
 * @Date 2024/9/3  13:04
 **/
@Data

public class Draw implements Serializable {
    private static final long serialVersionUID = 1L;


    /**
     * 期数
     */

    private Long period;

    /**
     * 红球
     */

    private List<Long> redBalls;

    /**
     * 蓝球
     */

    private Long blueBall;


    /**
     * 开奖日期
     */

    private String datetime;

    /**
     * 周几
     */

    private String dayOfWeek;
}
