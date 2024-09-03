package com.tiankong44.tool.lotto.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * @Description :
 * @Author zhanghao_SMEICS
 * @Date 2024/9/3  13:04
 **/
@Data
@TableName("history")
public class Lotto implements Serializable {
    private static final long serialVersionUID = 1L;


    private Long id;

    /**
     * 期数
     */

    private Long period;

    /**
     * 红球1
     */

    private Long redOne;

    /**
     * 红球2
     */

    private Long redTwo;

    /**
     * 红球3
     */

    private Long redTree;

    /**
     * 红球4
     */

    private Long redFour;

    /**
     * 红球5
     */

    private Long redFive;

    /**
     * 红球6
     */

    private Long redSix;

    /**
     * 蓝球
     */

    private Long blueOne;

    /**
     * 开奖日期
     */

    private String datetime;

    /**
     * 周几
     */

    private String dayOfWeek;
}
