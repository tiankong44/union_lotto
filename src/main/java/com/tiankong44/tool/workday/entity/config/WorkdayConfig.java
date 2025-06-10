package com.tiankong44.tool.workday.entity.config;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * @TableName workday_config
 */
@TableName(value = "workday_config")
@Data
public class WorkdayConfig implements Serializable {


    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    /**
     *
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     *
     */
    private String workStatus;

    /**
     * 排班情况中文表述
     */
    private String workStatusDesc;

    /**
     * 工作时间段
     */
    private String workTime;

    /**
     *
     */
    private String icon;

    /**
     *
     */
    private String color;

    private String workDesc;

    private Integer status;
}