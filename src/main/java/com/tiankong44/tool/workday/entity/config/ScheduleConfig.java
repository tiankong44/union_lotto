package com.tiankong44.tool.workday.entity.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * n
 *
 * @Description :
 * @Author zhanghao_SMEICS
 * @Date 2025/5/27  11:12
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleConfig {
    private String status;
    private String description;
    private String workTime;
    private String icon;
    private String color;
}
