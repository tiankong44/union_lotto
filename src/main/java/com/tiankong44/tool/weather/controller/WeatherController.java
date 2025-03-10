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

    @PostMapping("/getHourlyWeather")
    public BaseRes getHourlyWeather() {

        return weatherService.getHourlyWeatherParsed("101040700");

    }

    @PostMapping("/getMinutelyWeather")
    public BaseRes getMinutelyWeather() {

        return weatherService.getMinutelyWeatherParsed("106.527538", "29.708000");

    }

    @PostMapping("/getSevenDaysWeather")
    public BaseRes getSevenDaysWeather() {

        return weatherService.getSevenDaysWeatherParsed("101040700");


    }
}
