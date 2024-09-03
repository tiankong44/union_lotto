package com.tiankong44.tool.util;

import javax.validation.constraints.NotNull;

/**
 * @author zhanghao_SMEICS
 * @date 2022-10-21 23:04
 */
public class StringUtil {
    /***
     * 将此字符串去除指定的后缀结尾
     * @param str
     * @param suffix
     * @return
     */
    public static String trimEnd(@NotNull String str, String suffix) {
        if (str.endsWith(suffix)) {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }


}
