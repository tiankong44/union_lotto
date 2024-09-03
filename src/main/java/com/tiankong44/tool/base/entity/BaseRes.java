package com.tiankong44.tool.base.entity;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**

 * @Description :
 * @Author zhanghao_SMEICS
 * @Date 2022/10/21  16:24
 **/
@Data
@Builder
public class BaseRes<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    // 返回状态码
    private int code; //0 成功 1 失败  999 需要登录访问
    // 描述
    private String message;
    // 返回对象
    private T data;

    private static final BaseRes EMPTY_SUCCESS_RESULT = BaseRes.success(null);

    public static <T> BaseRes<T> success() {
        return EMPTY_SUCCESS_RESULT;
    }

    public static <T> BaseRes<T> success(T obj) {
        BaseResBuilder<T> resultBuilder = new BaseResBuilder<T>();
        return resultBuilder
                .data(obj)
                .code(CommonResultStatus.OK.getCode())
                .message(CommonResultStatus.OK.getMessage()).build();
    }

    public static BaseRes failure(ResultStatus resultStatus) {
        return BaseRes.builder()
                .code(resultStatus.getCode())
                .message(resultStatus.getMessage()).build();
    }

    public static BaseRes failure(ResultStatus resultStatus, Throwable e) {
        return BaseRes.builder()
                .data(e)
                .code(resultStatus.getCode())
                .message(resultStatus.getMessage()).build();
    }

    public static BaseRes failure(ResultStatus resultStatus, String message) {
        return BaseRes.builder()
                .code(resultStatus.getCode())
                .message(message).build();
    }

    public static BaseRes failure(ResultStatus resultStatus, ResultStatus code) {
        return BaseRes.builder()
                .code(code.getCode())
                .message(code.getMessage()).build();
    }

    public static BaseRes failure(ResultStatus resultStatus, int code, String message) {
        return BaseRes.builder()
                .code(code)
                .message(message).build();
    }

    public static BaseRes failure(String message) {
        return BaseRes.builder()
                .code(1)
                .message(message).build();
    }
}
