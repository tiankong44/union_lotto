package com.tiankong44.tool.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**

 * @Description :
 * @Author zhanghao_SMEICS
 * @Date 2024/10/31  15:02
 **/

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE}) // 支持类和方法级别
public @interface SkipLogging {
}