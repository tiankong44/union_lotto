package com.tiankong44.tool.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tiankong44.tool.weather.entity.DailyWeather;
import com.tiankong44.tool.weather.entity.HourlyWeather;
import com.tiankong44.tool.weather.entity.RealTimeWeather;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

@Component
public class HeFengWeatherClient {
    private static final String API_URL_NOW = "https://devapi.qweather.com/v7/weather/now";
    private static final String API_URL_MINUTELY = "https://devapi.qweather.com/v7/minutely/5m";
    private static final String API_URL_HOURLY = "https://devapi.qweather.com/v7/weather/24h";
    private static final String API_URL_DAILY = "https://devapi.qweather.com/v7/weather/7d";

    @Autowired
    private JwtTokenManager jwtTokenManager;

    /**
     * 获取指定城市的实时天气数据（原始 JSON）
     *
     * @param location 城市代码（如101010100）
     * @return 天气数据的JSON字符串
     */
    public String getRealTimeWeather(String location) {
        String url = API_URL_NOW + "?location=" + location;
        return HttpUtils.get(url, jwtTokenManager.getValidJWT());
    }

    /**
     * 获取指定城市的实时天气数据（解析后）
     *
     * @param location 城市代码（如101010100）
     * @return 解析后的天气信息
     */
    public JSONObject getRealTimeWeatherParsed(String location) {
        String jsonData = getRealTimeWeather(location);
        return parseRealTimeWeather(jsonData);
    }

    /**
     * 获取指定城市的分钟级降水预报（原始 JSON）
     * GCJ-02坐标系
     *
     * @param longitude
     * @param latitude
     * @return 降水预报的JSON字符串
     */
    public String getMinutelyWeather(String longitude, String latitude) {
        String url = API_URL_MINUTELY + "?location=" + longitude + "," + latitude;
        return HttpUtils.get(url, jwtTokenManager.getValidJWT());
    }

    /**
     * 获取指定城市的分钟级降水预报（解析后）
     * GCJ-02坐标系
     *
     * @param longitude
     * @param latitude
     * @return 解析后的降水信息
     */
    public JSONObject getMinutelyWeatherParsed(String longitude, String latitude) {
        String jsonData = getMinutelyWeather(longitude, latitude);
        return parseMinutelyWeather(jsonData);
    }

    /**
     * 获取指定城市的逐小时天气预报（原始 JSON）
     *
     * @param location 城市代码（如101010100）
     * @return 逐小时天气预报的JSON字符串
     */
    public String getHourlyWeather(String location) {
        String url = API_URL_HOURLY + "?location=" + location;
        return HttpUtils.get(url, jwtTokenManager.getValidJWT());
    }

    /**
     * 获取指定城市的逐小时天气预报（解析后）
     *
     * @param location 城市代码（如101010100）
     * @return 解析后的逐小时天气信息
     */
    public JSONObject getHourlyWeatherParsed(String location) {
        String jsonData = getHourlyWeather(location);
        return parseHourlyWeather(jsonData);
    }

    /**
     * 获取指定城市的每日天气预报（原始 JSON）
     *
     * @param location 城市代码（如101010100）
     * @return 每日天气预报的JSON字符串
     */
    public String getSevenDaysWeather(String location) {
        String url = API_URL_DAILY + "?location=" + location;
        return HttpUtils.get(url, jwtTokenManager.getValidJWT());
    }

    /**
     * 获取指定城市的每日天气预报（解析后）
     *
     * @param location 城市代码（如101010100）
     * @return 解析后的每日天气信息
     */
    public JSONObject getSevenDaysWeatherParsed(String location) {
        String jsonData = getSevenDaysWeather(location);
        return parseDailyWeather(jsonData);
    }

    /**
     * 解析实时天气数据
     *
     * @param json 天气数据的JSON字符串
     * @return 解析后的天气信息
     */

    public JSONObject parseRealTimeWeather(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }

        JSONObject parseJSONObject = new JSONObject();
        JSONObject jsonObject = JSON.parseObject(json);

        // 检查状态码
        if (jsonObject.containsKey("code") && !jsonObject.getString("code").equals("200")) {
            parseJSONObject.put("Error", jsonObject.getString("code"));
            parseJSONObject.put("Message", jsonObject.getString("msg"));
            return parseJSONObject;
        }

        // 解析实时天气数据
        JSONObject now = jsonObject.getJSONObject("now");
        RealTimeWeather realTimeWeather = new RealTimeWeather();

        realTimeWeather.setObsTime(now.getString("obsTime"));
        realTimeWeather.setTemp(now.getString("temp"));
        realTimeWeather.setFeelsLike(now.getString("feelsLike"));
        realTimeWeather.setIcon(now.getString("icon"));
        realTimeWeather.setText(now.getString("text"));
        realTimeWeather.setWind360(now.getString("wind360"));
        realTimeWeather.setWindDir(now.getString("windDir"));
        realTimeWeather.setWindScale(now.getString("windScale"));
        realTimeWeather.setWindSpeed(now.getString("windSpeed"));
        realTimeWeather.setHumidity(now.getString("humidity"));
        realTimeWeather.setPrecip(now.getString("precip"));
        realTimeWeather.setPressure(now.getString("pressure"));
        realTimeWeather.setVis(now.getString("vis"));
        realTimeWeather.setCloud(now.getString("cloud"));
        realTimeWeather.setDew(now.getString("dew"));

        // 将实体类转换为 JSON 对象
        parseJSONObject.put("realTimeWeather", JSON.toJSON(realTimeWeather));

        // 添加其他信息
        parseJSONObject.put("updateTime", jsonObject.getString("updateTime"));
        parseJSONObject.put("fxLink", jsonObject.getString("fxLink"));
        parseJSONObject.put("refer", jsonObject.getJSONObject("refer"));

        return parseJSONObject;
    }

    /**
     * 解析分钟级降水预报
     *
     * @param json 降水预报的JSON字符串
     * @return 解析后的降水信息
     */
    public JSONObject parseMinutelyWeather(String json) {
        JSONObject parseJSONObject = null;
        if (json == null || json.isEmpty()) {
            return parseJSONObject;
        }
        parseJSONObject=new JSONObject();
        JSONObject jsonObject = JSON.parseObject(json);
        if (jsonObject.containsKey("code") && !jsonObject.getString("code").equals("200")) {
            parseJSONObject.put("Error", jsonObject.getString("code"));
            parseJSONObject.put("Message", jsonObject.getString("msg"));
            return parseJSONObject;

        }


        String summary = jsonObject.getString("summary");
        parseJSONObject.put("Summary", summary);
        return parseJSONObject;
    }

    /**
     * 解析逐小时天气预报
     *
     * @param json 逐小时天气预报的JSON字符串
     * @return 解析后的逐小时天气信息
     */
    public JSONObject parseHourlyWeather(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }

        JSONObject parseJSONObject = new JSONObject();
        JSONObject jsonObject = JSON.parseObject(json);

        // 检查状态码
        if (jsonObject.containsKey("code") && !jsonObject.getString("code").equals("200")) {
            parseJSONObject.put("Error", jsonObject.getString("code"));
            parseJSONObject.put("Message", jsonObject.getString("msg"));
            return parseJSONObject;
        }

        // 解析逐小时天气数据
        JSONArray hourlyArray = jsonObject.getJSONArray("hourly");
        List<HourlyWeather> hourlyWeatherList = new ArrayList<>();

        for (int i = 0; i < hourlyArray.size(); i++) {
            JSONObject hourly = hourlyArray.getJSONObject(i);
            HourlyWeather hourlyWeather = new HourlyWeather();

            hourlyWeather.setFxTime(hourly.getString("fxTime"));
            hourlyWeather.setTemp(hourly.getString("temp"));
            hourlyWeather.setIcon(hourly.getString("icon"));
            hourlyWeather.setText(hourly.getString("text"));
            hourlyWeather.setWind360(hourly.getString("wind360"));
            hourlyWeather.setWindDir(hourly.getString("windDir"));
            hourlyWeather.setWindScale(hourly.getString("windScale"));
            hourlyWeather.setWindSpeed(hourly.getString("windSpeed"));
            hourlyWeather.setHumidity(hourly.getString("humidity"));
            hourlyWeather.setPrecip(hourly.getString("precip"));
            hourlyWeather.setPop(hourly.getString("pop"));
            hourlyWeather.setPressure(hourly.getString("pressure"));
            hourlyWeather.setCloud(hourly.getString("cloud"));
            hourlyWeather.setDew(hourly.getString("dew"));

            hourlyWeatherList.add(hourlyWeather);
        }

        // 将实体列表转换为 JSON 数组
        JSONArray parsedHourlyArray = new JSONArray();
        for (HourlyWeather weather : hourlyWeatherList) {
            parsedHourlyArray.add(JSON.toJSON(weather));
        }

        // 添加解析后的逐小时天气数据
        parseJSONObject.put("hourly", parsedHourlyArray);

        // 添加其他信息
        parseJSONObject.put("updateTime", jsonObject.getString("updateTime"));
        parseJSONObject.put("fxLink", jsonObject.getString("fxLink"));
        parseJSONObject.put("refer", jsonObject.getJSONObject("refer"));

        return parseJSONObject;
    }
    /**
     * 解析每日天气预报
     *
     * @param json 每日天气预报的JSON字符串
     * @return 解析后的每日天气信息
     */
    public JSONObject parseDailyWeather(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }

        JSONObject parseJSONObject = new JSONObject();
        JSONObject jsonObject = JSON.parseObject(json);

        // 检查状态码
        if (jsonObject.containsKey("code") && !jsonObject.getString("code").equals("200")) {
            parseJSONObject.put("Error", jsonObject.getString("code"));
            parseJSONObject.put("Message", jsonObject.getString("msg"));
            return parseJSONObject;
        }

        // 解析每日天气数据
        JSONArray dailyArray = jsonObject.getJSONArray("daily");
        List<DailyWeather> dailyWeatherList = new ArrayList<>();

        for (int i = 0; i < dailyArray.size(); i++) {
            JSONObject daily = dailyArray.getJSONObject(i);
            DailyWeather dailyWeather = new DailyWeather();

            dailyWeather.setFxDate(daily.getString("fxDate"));
            dailyWeather.setSunrise(daily.getString("sunrise"));
            dailyWeather.setSunset(daily.getString("sunset"));
            dailyWeather.setMoonrise(daily.getString("moonrise"));
            dailyWeather.setMoonset(daily.getString("moonset"));
            dailyWeather.setMoonPhase(daily.getString("moonPhase"));
            dailyWeather.setMoonPhaseIcon(daily.getString("moonPhaseIcon"));
            dailyWeather.setTempMax(daily.getString("tempMax"));
            dailyWeather.setTempMin(daily.getString("tempMin"));
            dailyWeather.setIconDay(daily.getString("iconDay"));
            dailyWeather.setTextDay(daily.getString("textDay"));
            dailyWeather.setIconNight(daily.getString("iconNight"));
            dailyWeather.setTextNight(daily.getString("textNight"));
            dailyWeather.setWind360Day(daily.getString("wind360Day"));
            dailyWeather.setWindDirDay(daily.getString("windDirDay"));
            dailyWeather.setWindScaleDay(daily.getString("windScaleDay"));
            dailyWeather.setWindSpeedDay(daily.getString("windSpeedDay"));
            dailyWeather.setWind360Night(daily.getString("wind360Night"));
            dailyWeather.setWindDirNight(daily.getString("windDirNight"));
            dailyWeather.setWindScaleNight(daily.getString("windScaleNight"));
            dailyWeather.setWindSpeedNight(daily.getString("windSpeedNight"));
            dailyWeather.setHumidity(daily.getString("humidity"));
            dailyWeather.setPrecip(daily.getString("precip"));
            dailyWeather.setPressure(daily.getString("pressure"));
            dailyWeather.setVis(daily.getString("vis"));
            dailyWeather.setCloud(daily.getString("cloud"));
            dailyWeather.setUvIndex(daily.getString("uvIndex"));

            dailyWeatherList.add(dailyWeather);
        }

        // 将实体列表转换为 JSON 数组
        JSONArray parsedDailyArray = new JSONArray();
        for (DailyWeather weather : dailyWeatherList) {
            parsedDailyArray.add(JSON.toJSON(weather));
        }

        // 添加解析后的每日天气数据
        parseJSONObject.put("daily", parsedDailyArray);

        // 添加其他信息
        parseJSONObject.put("updateTime", jsonObject.getString("updateTime"));
        parseJSONObject.put("fxLink", jsonObject.getString("fxLink"));
        parseJSONObject.put("refer", jsonObject.getJSONObject("refer"));

        return parseJSONObject;
    }

}