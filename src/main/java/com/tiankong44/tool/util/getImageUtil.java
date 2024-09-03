package com.tiankong44.tool.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tiankong44.tool.common.entity.BingImage;
import com.tiankong44.tool.common.entity.ClientEnum;
import com.tiankong44.tool.common.entity.Image;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description :
 * @Author zhanghao_SMEICS
 * @Date 2022/11/2  16:08
 **/
public class getImageUtil {
    private static String BING_URL = "https://cn.bing.com/HPImageArchive.aspx?";  // 返回格式 json
    private static String BING_FORMAT = "js";  // 返回格式 json
    private static String BING_MKT = "zh-CN"; // 地区
    private static String BING_IDX = "0";  //请求图片截止天数
    private static String BING_NUM = "8";  //请求数量
    private static String BING_PREFIX = "https://cn.bing.com";  // 图片地址前缀

    private static String PICASSO_URL = "http://service.picasso.adesk.com/v1/vertical/category/4e4d610cdf714d2966000002/vertical?limit=30&adult=false&first=1&order=new";  // 返回格式 json

    public static List<Image> getImages(int clientType) {
        if (clientType == ClientEnum.PC.getValue()) {
            return getBingImages();
        } else if (clientType == ClientEnum.MOBILE.getValue()) {
            return getMobileImages();
        } else {
            return getBingImages();
        }
    }

    public static List<Image> getBingImages() {

        RestTemplate restTemplate = new RestTemplate();
        Map<String, String> map = new HashMap<String, String>();
        map.put("format", BING_FORMAT);
        map.put("mkt", BING_MKT);
        map.put("idx", BING_IDX);
        map.put("n", BING_NUM);
        String img = restTemplate.getForObject(BING_URL + "format={format}&idx={idx}&n={n}&mkt={mkt}", String.class, map);
        JSONObject jsonObject = JSONObject.parseObject(img);
        // 下面 这个数组里面就是获取到的壁纸集合信息
        JSONArray array = jsonObject.getJSONArray("images");

        String content;
        List<Image> images = new ArrayList<Image>();
        for (Object o : array) {
            Image image = new Image();
            BingImage bingImage = JSONObject.toJavaObject((JSON) JSONObject.toJSON(o), BingImage.class);
            image.setUrl(BING_PREFIX + bingImage.getUrl());
            content = bingImage.getCopyright();
            if (content.length() > 0) {
                String addr = content.substring(0, content.indexOf("("));
                image.setCopyright(addr);
                images.add(image);
            }
        }
        return images;
    }


    public static List<Image> getMobileImages() {

        RestTemplate restTemplate = new RestTemplate();

        String img = restTemplate.getForObject(PICASSO_URL, String.class);
        JSONObject jsonObject = JSONObject.parseObject(img);
        Integer code = jsonObject.getInteger("code");
        if (code.equals(0)) {
            JSONObject res = jsonObject.getJSONObject("res");
            JSONArray array = res.getJSONArray("vertical");
            List<Image> images = new ArrayList<Image>();
            for (Object o : array) {
                Image image = new Image();
                JSONObject obj = (JSONObject) JSONObject.toJSON(o);
                image.setUrl(obj.getString("img"));
                images.add(image);
            }
            return images;
        } else {
            return new ArrayList<>();
        }
        // 下面 这个数组里面就是获取到的壁纸集合信息


    }
}
