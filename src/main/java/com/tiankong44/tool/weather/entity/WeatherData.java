package com.tiankong44.tool.weather.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @param user
 * @param msg
 * @Description :
 * @Author zhanghao_SMEICS
 * @Date 2025/3/11  11:00
 **/

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherData {
    private String week1;
    private String week2;
    private String wea1;
    private String wea2;
    private String wendu1;
    private String wendu2;
    private String img1;
    private String img2;
}
