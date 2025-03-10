package com.tiankong44.tool.weather.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Description :
 * @Author zhanghao_SMEICS
 * @Date 2025/3/10  16:05
 **/

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HourlyWeather {
    /**
     * 预报时间
     */
    @JSONField(name = "fxTime")
    private String fxTime;

    /**
     * 温度，默认单位：摄氏度
     */
    @JSONField(name = "temp")
    private String temp;

    /**
     * 天气状况的图标代码
     */
    @JSONField(name = "icon")
    private String icon;

    /**
     * 天气状况的文字描述
     */
    @JSONField(name = "text")
    private String text;

    /**
     * 风向360角度
     */
    @JSONField(name = "wind360")
    private String wind360;

    /**
     * 风向
     */
    @JSONField(name = "windDir")
    private String windDir;

    /**
     * 风力等级
     */
    @JSONField(name = "windScale")
    private String windScale;

    /**
     * 风速，公里/小时
     */
    @JSONField(name = "windSpeed")
    private String windSpeed;

    /**
     * 相对湿度，百分比数值
     */
    @JSONField(name = "humidity")
    private String humidity;

    /**
     * 当前小时累计降水量，默认单位：毫米
     */
    @JSONField(name = "precip")
    private String precip;

    /**
     * 逐小时预报降水概率，百分比数值
     */
    @JSONField(name = "pop")
    private String pop;

    /**
     * 大气压强，默认单位：百帕
     */
    @JSONField(name = "pressure")
    private String pressure;

    /**
     * 云量，百分比数值
     */
    @JSONField(name = "cloud")
    private String cloud;

    /**
     * 露点温度
     */
    @JSONField(name = "dew")
    private String dew;
}
