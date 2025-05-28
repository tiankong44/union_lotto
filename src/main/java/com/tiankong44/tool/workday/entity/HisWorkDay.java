package com.tiankong44.tool.workday.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * @Description :
 * @Author zhanghao_SMEICS
 * @Date 2024/9/3  13:04
 **/
@Data
@TableName("his_workday")
public class HisWorkDay implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 日期
     */

    private String  workDate;

    /**
     *排班符号代表
     */

    private String workStatus;

    /**
     * 状态
     */

    private Integer status;

    /**
     * 排班情况中文表述
     */

    private String workStatusDesc;

    /**
     * 工作时间段
     */

    private String workTime;


    private String icon;
    private String color;

}
