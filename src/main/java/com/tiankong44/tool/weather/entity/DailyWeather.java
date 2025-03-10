package com.tiankong44.tool.weather.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Description :
 * @Author zhanghao_SMEICS
 * @Date 2025/3/10  15:57
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailyWeather {
    /**
     * 预报日期
     */
    @JSONField(name = "fxDate")
    private String fxDate;

    /**
     * 日出时间，在高纬度地区可能为空
     */
    @JSONField(name = "sunrise")
    private String sunrise;

    /**
     * 日落时间，在高纬度地区可能为空
     */
    @JSONField(name = "sunset")
    private String sunset;

    /**
     * 当天月升时间，可能为空
     */
    @JSONField(name = "moonrise")
    private String moonrise;

    /**
     * 当天月落时间，可能为空
     */
    @JSONField(name = "moonset")
    private String moonset;

    /**
     * 月相名称
     */
    @JSONField(name = "moonPhase")
    private String moonPhase;

    /**
     * 月相图标代码，另请参考天气图标项目
     */
    @JSONField(name = "moonPhaseIcon")
    private String moonPhaseIcon;

    /**
     * 预报当天最高温度
     */
    @JSONField(name = "tempMax")
    private String tempMax;

    /**
     * 预报当天最低温度
     */
    @JSONField(name = "tempMin")
    private String tempMin;

    /**
     * 预报白天天气状况的图标代码，另请参考天气图标项目
     */
    @JSONField(name = "iconDay")
    private String iconDay;

    /**
     * 预报白天天气状况文字描述，包括阴晴雨雪等天气状态的描述
     */
    @JSONField(name = "textDay")
    private String textDay;

    /**
     * 预报夜间天气状况的图标代码，另请参考天气图标项目
     */
    @JSONField(name = "iconNight")
    private String iconNight;

    /**
     * 预报晚间天气状况文字描述，包括阴晴雨雪等天气状态的描述
     */
    @JSONField(name = "textNight")
    private String textNight;

    /**
     * 预报白天风向360角度
     */
    @JSONField(name = "wind360Day")
    private String wind360Day;

    /**
     * 预报白天风向
     */
    @JSONField(name = "windDirDay")
    private String windDirDay;

    /**
     * 预报白天风力等级
     */
    @JSONField(name = "windScaleDay")
    private String windScaleDay;

    /**
     * 预报白天风速，公里/小时
     */
    @JSONField(name = "windSpeedDay")
    private String windSpeedDay;

    /**
     * 预报夜间风向360角度
     */
    @JSONField(name = "wind360Night")
    private String wind360Night;

    /**
     * 预报夜间当天风向
     */
    @JSONField(name = "windDirNight")
    private String windDirNight;

    /**
     * 预报夜间风力等级
     */
    @JSONField(name = "windScaleNight")
    private String windScaleNight;

    /**
     * 预报夜间风速，公里/小时
     */
    @JSONField(name = "windSpeedNight")
    private String windSpeedNight;

    /**
     * 相对湿度，百分比数值
     */
    @JSONField(name = "humidity")
    private String humidity;

    /**
     * 预报当天总降水量，默认单位：毫米
     */
    @JSONField(name = "precip")
    private String precip;

    /**
     * 大气压强，默认单位：百帕
     */
    @JSONField(name = "pressure")
    private String pressure;

    /**
     * 能见度，默认单位：公里
     */
    @JSONField(name = "vis")
    private String vis;

    /**
     * 云量，百分比数值。可能为空
     */
    @JSONField(name = "cloud")
    private String cloud;

    /**
     * 紫外线强度指数
     */
    @JSONField(name = "uvIndex")
    private String uvIndex;
}