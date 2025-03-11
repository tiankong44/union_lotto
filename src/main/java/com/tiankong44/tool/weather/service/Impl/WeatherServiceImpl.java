package com.tiankong44.tool.weather.service.Impl;

import com.alibaba.fastjson.JSONObject;
import com.tiankong44.tool.base.entity.BaseRes;
import com.tiankong44.tool.util.ApiBoxUtil;
import com.tiankong44.tool.util.HeFengWeatherClient;
import com.tiankong44.tool.weather.entity.WeatherResponse;
import com.tiankong44.tool.weather.service.WeatherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Description :
 * @Author zhanghao_SMEICS
 * @Date 2022/11/2  16:35
 **/
@Service
public class WeatherServiceImpl implements WeatherService {
    @Autowired
    HeFengWeatherClient heFengWeatherClient;
    @Autowired
    ApiBoxUtil apiBoxUtil;

    /**
     * 获取指定城市的实时天气数据（解析后）
     *
     * @param location 城市代码（如101010100）
     * @return 解析后的天气信息
     */
    @Override
    public BaseRes getRealTimeWeatherParsed(String location) {
        JSONObject realTimeWeather = heFengWeatherClient.getRealTimeWeatherParsed(location);
        if (realTimeWeather == null) {
            return BaseRes.failure("Invalid");
        } else {
            return BaseRes.success(realTimeWeather);
        }
    }

    /**
     * 获取指定城市的逐小时天气预报（解析后）
     *
     * @param location 城市代码（如101010100）
     * @return 解析后的逐小时天气信息
     */

    @Override
    public BaseRes getHourlyWeatherParsed(String location) {
        JSONObject realTimeWeather = heFengWeatherClient.getHourlyWeatherParsed(location);
        if (realTimeWeather == null) {
            return BaseRes.failure("Invalid");
        } else {
            return BaseRes.success(realTimeWeather);
        }
    }

    /**
     * 获取指定城市的分钟级降水预报（解析后）
     * GCJ-02坐标系
     *
     * @param longitude
     * @param latitude
     * @return 解析后的降水信息
     */
    @Override
    public BaseRes getMinutelyWeatherParsed(String longitude, String latitude) {
        JSONObject realTimeWeather = heFengWeatherClient.getMinutelyWeatherParsed(longitude, latitude);
        if (realTimeWeather == null) {
            return BaseRes.failure("Invalid");
        } else {
            return BaseRes.success(realTimeWeather);
        }
    }

    /**
     * 获取指定城市的每日天气预报（解析后）
     *
     * @param location 城市代码（如101010100）
     * @return 解析后的每日天气信息
     */
    @Override
    public BaseRes getSevenDaysWeatherParsed(String location) {
        JSONObject realTimeWeather = heFengWeatherClient.getSevenDaysWeatherParsed(location);
        if (realTimeWeather == null) {
            return BaseRes.failure("Invalid");
        } else {
            return BaseRes.success(realTimeWeather);
        }
    }

    @Override
    public BaseRes getFuture15DaysWeather(String sheng, String place) {

        WeatherResponse future15DaysWeather = apiBoxUtil.getFuture15DaysWeather(sheng, place);
        if (future15DaysWeather == null) {
            return BaseRes.failure("Invalid");
        } else {
            return BaseRes.success(future15DaysWeather);
        }

    }
}
