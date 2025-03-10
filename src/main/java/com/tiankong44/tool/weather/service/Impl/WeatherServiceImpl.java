package com.tiankong44.tool.weather.service.Impl;

import com.alibaba.fastjson.JSONObject;
import com.tiankong44.tool.base.entity.BaseRes;
import com.tiankong44.tool.util.HeFengWeatherClient;
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

    @Override
    public BaseRes getHourlyWeatherParsed(String location) {
        JSONObject realTimeWeather = heFengWeatherClient.getHourlyWeatherParsed(location);
        if (realTimeWeather == null) {
            return BaseRes.failure("Invalid");
        } else {
            return BaseRes.success(realTimeWeather);
        }
    }

    @Override
    public BaseRes getMinutelyWeatherParsed(String longitude, String latitude) {
        JSONObject realTimeWeather = heFengWeatherClient.getMinutelyWeatherParsed(longitude, latitude);
        if (realTimeWeather == null) {
            return BaseRes.failure("Invalid");
        } else {
            return BaseRes.success(realTimeWeather);
        }
    }

    @Override
    public BaseRes getSevenDaysWeatherParsed(String location) {
        JSONObject realTimeWeather = heFengWeatherClient.getSevenDaysWeatherParsed(location);
        if (realTimeWeather == null) {
            return BaseRes.failure("Invalid");
        } else {
            return BaseRes.success(realTimeWeather);
        }
    }
}
