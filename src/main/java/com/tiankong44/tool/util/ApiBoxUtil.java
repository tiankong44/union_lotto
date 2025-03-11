package com.tiankong44.tool.util;

import com.alibaba.fastjson.JSON;
import com.tiankong44.tool.weather.entity.ApiBoxApiResponse;
import com.tiankong44.tool.weather.entity.WeatherResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @Description :
 * @Author zhanghao_SMEICS
 * @Date 2025/3/11  10:44
 **/
@Component
public class ApiBoxUtil {

    private static final String API_URL = "https://api.apihz.cn/getapi.php";
    private static final String API_BAK_URL = "https://cn.apihz.cn/";
    private static final String WEATHER_API_ID = "10003143";
    private static final String WEATHER_API_KEY = "fbe7ab413aba8cf92d837ff725bbce8f";
    /**
     * 获取接口返回的API地址
     *
     * @return 返回的API地址
     */
    public String getApiAddress() {
        try {
            // 使用HttpUtils发送GET请求
            String response = HttpUtils.get(API_URL, null); // 假设不需要JWT认证
            if (response == null) {
                throw new RuntimeException("请求接口失败，未获取到响应");
            }

            // 将返回的JSON字符串反序列化为ApiResponse对象
            ApiBoxApiResponse apiResponse = JSON.parseObject(response, ApiBoxApiResponse.class);

            // 检查返回的状态码
            if (apiResponse.getCode() == 200) {
                // 获取并返回API地址
                return apiResponse.getApi();
            } else {
                throw new RuntimeException("API请求失败，返回状态码：" + apiResponse.getCode() + "，消息：" + apiResponse.getMsg());
            }
        } catch (Exception e) {
            throw new RuntimeException("获取API地址失败", e);
        }

    }

    /**
     * 查询未来15天的天气信息
     *
     * @param sheng 省份
     * @param place 地点
     * @return WeatherResponse对象
     */
    public WeatherResponse getFuture15DaysWeather(String sheng, String place) {
        try {
            // 获取API地址
            String baseApiUrl = getApiAddress();
            if (StringUtils.isEmpty(baseApiUrl)) {
                baseApiUrl = API_BAK_URL;
            }
            // 构造完整的天气查询URL
            // 构造完整的天气查询URL
            String weatherUrl = baseApiUrl + "/api/tianqi/tqybmoji15.php?id=" + WEATHER_API_ID +
                    "&key=" + WEATHER_API_KEY +
                    "&sheng=" + sheng +
                    "&place=" + place;
            // 使用HttpUtils发送GET请求
            String response = HttpUtils.get(weatherUrl, null); // 假设不需要JWT认证
            if (response == null) {
                throw new RuntimeException("请求天气接口失败，未获取到响应");
            }

            // 将返回的JSON字符串反序列化为WeatherResponse对象
            WeatherResponse weatherResponse = JSON.parseObject(response, WeatherResponse.class);

            // 检查返回的状态码
            if (weatherResponse.getCode() == 200) {
                return weatherResponse;
            } else {
                throw new RuntimeException("天气查询失败，返回状态码：" + weatherResponse.getCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("查询未来15天天气信息失败", e);
        }
    }

}