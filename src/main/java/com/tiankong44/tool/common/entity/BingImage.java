package com.tiankong44.tool.common.entity;

import lombok.Data;

import java.util.List;

/**
 * @Description :
 * @Author zhanghao_SMEICS
 * @Date 2022/11/2  16:12
 **/
@Data
public class BingImage {
    private String startDate;
    private String fullStartDate;
    private String endDate;
    private String url;
    private String urlBase;
    private String copyright;
    private String copyrightLink;
    private String title;
    private String quiz;
    private boolean wp;
    private String hsh;
    private int drk;
    private int top;
    private int bot;
    private List<String> hs;
}
