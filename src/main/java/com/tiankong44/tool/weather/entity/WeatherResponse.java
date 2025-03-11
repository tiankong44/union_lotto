package com.tiankong44.tool.weather.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**

 * @Description :
 * @Author zhanghao_SMEICS
 * @Date 2025/3/11  10:59
 **/

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherResponse {

    private List<WeatherData> data;
    private int code;
    private String place;
}
