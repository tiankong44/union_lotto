package com.tiankong44.tool.weather.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口盒子api优选返回值
 *
 * @Description :
 * @Author zhanghao_SMEICS
 * @Date 2025/3/11  10:49
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiBoxApiResponse {
    private int code;
    private String msg;
    private String api;
}
