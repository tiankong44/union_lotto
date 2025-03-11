package com.tiankong44.tool.weather.service;

import com.alibaba.fastjson.JSONObject;
import com.tiankong44.tool.base.entity.BaseRes;

/**
 * @Description :
 * @Author zhanghao_SMEICS
 * @Date 2022/11/2  16:34
 **/
public interface WeatherService {

    /**
     * 获取指定城市的实时天气数据（解析后）
     *
     * @param location 城市代码（如101010100）
     * @return 解析后的天气信息
     */
    BaseRes getRealTimeWeatherParsed(String location);

    BaseRes getHourlyWeatherParsed(String location);

    BaseRes getMinutelyWeatherParsed(String longitude, String latitude);

    BaseRes getSevenDaysWeatherParsed(String location);

    BaseRes getFuture15DaysWeather(String sheng, String place);
}
