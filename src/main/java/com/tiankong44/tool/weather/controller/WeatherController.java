package com.tiankong44.tool.weather.controller;

import com.alibaba.fastjson.JSONObject;
import com.tiankong44.tool.base.entity.BaseRes;
import com.tiankong44.tool.util.HeFengWeatherClient;
import com.tiankong44.tool.util.JwtTokenManager;
import com.tiankong44.tool.weather.service.WeatherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

/**
 * @Description :
 * @Author zhanghao_SMEICS
 * @Date 2024/9/3  13:04
 **/
@RestController
@RequestMapping("weather")
public class WeatherController {
    @Resource
    WeatherService weatherService;

    /**
     * 获取指定城市的实时天气数据（解析后）
     *
     * @return 解析后的天气信息
     */
    @PostMapping("/getNow")
    public BaseRes getNow() {
        return weatherService.getRealTimeWeatherParsed("101040700");
    }

    /**
     * 获取指定城市的逐小时天气预报（原始 JSON）
     *
     * @return 逐小时天气预报的JSON字符串
     */
    @PostMapping("/getHourlyWeather")
    public BaseRes getHourlyWeather() {
        return weatherService.getHourlyWeatherParsed("101040700");
    }

    /**
     * 获取指定城市的分钟级降水预报（解析后）
     * GCJ-02坐标系
     *
     * @return 解析后的降水信息
     */
    @PostMapping("/getMinutelyWeather")
    public BaseRes getMinutelyWeather() {
        return weatherService.getMinutelyWeatherParsed("106.527538", "29.708000");
    }

    /**
     * 获取指定城市的每日天气预报（解析后）
     *
     * @return 解析后的每日天气信息
     */
    @PostMapping("/getSevenDaysWeather")
    public BaseRes getSevenDaysWeather() {
        return weatherService.getSevenDaysWeatherParsed("101040700");
    }

    /**
     * 获取指定城市的每日天气预报（解析后）
     *
     * @return 解析后的每日天气信息
     */
    @PostMapping("/getFuture15DaysWeather")
    public BaseRes getFuture15DaysWeather() {
        return weatherService.getFuture15DaysWeather("重庆","渝北");
    }
}
