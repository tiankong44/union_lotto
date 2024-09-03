package com.tiankong44.tool.exception.customException;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zhanghao_SMEICS
 * @date 2022-10-22 16:04
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DifferentCoordinateException extends Exception {
    //异常信息
    private String message;
    //错误位置
    private String location;
}
