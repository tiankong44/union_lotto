package com.tiankong44.tool.gis.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * @author zhanghao_SMEICS
 * @date 2022-10-22 13:55
 */
@ToString
@AllArgsConstructor
public enum CoordinateTypeEnum {
    GPS84(0),
    GCJ02(1),
    BD09(2);
    @Getter
    public final int value;

    public static CoordinateTypeEnum getByValue(Integer value) {
        for (CoordinateTypeEnum e: CoordinateTypeEnum.values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }

}
