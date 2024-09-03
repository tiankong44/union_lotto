package com.tiankong44.tool.common.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Description :
 * @Author zhanghao_SMEICS
 * @Date 2022/11/8  13:35
 **/

@Getter
@AllArgsConstructor
public enum ClientEnum {
    PC(1, "pcImageKey"),
    MOBILE(2, "mobileImageKey");

    private Integer value;
    private String label;


}
