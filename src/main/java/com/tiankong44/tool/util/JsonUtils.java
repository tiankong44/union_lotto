package com.tiankong44.tool.util;

import com.alibaba.fastjson.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Json工具类.
 *
 * @author miaoyi
 */
public class JsonUtils {

    /**
     * 判断jsonObject是否含有params，且不为空，不为null
     *
     * @param jsonObject
     * @param params
     * @return
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Map noNulls(JSONObject jsonObject, String... params) {
        String keys = "";
        for (String str : params) {
            String value = "";
            if (jsonObject.containsKey(str)) {
                value = jsonObject.get(str).toString();
            }

            if (value.trim().length() == 0) {
                keys += "," + str;
            }
        }
        if (keys.length() > 0) {
            Map retMap = new HashMap();
            retMap.put("status", "0");
            retMap.put("desc", "入参不正确" + keys + "不能为空");
            return retMap;
        }
        return null;
    }


}
