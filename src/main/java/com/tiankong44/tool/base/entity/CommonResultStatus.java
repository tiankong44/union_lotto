package com.tiankong44.tool.base.entity;


/**
 * @Author 胡彬
 * @Date 2021/6/21 14:54
 */
public enum CommonResultStatus implements ResultStatus{
    OK(0, "操作成功"),
    NOK(1,"操作失败"),
    INTERNAL_SERVER_ERROR(1001, "服务异常"),
    MISSING_PARAMETER(1002, "参数缺失"),
    NO_POWER(1003,"无权访问"),
    NO_AUTHORITY(1004,"无操作权限"),
    NO_PASSWORD(1005,"未设置密码"),
    TOKEN_TIMEOUT(1006,"token过期！请清除缓存后重新进入！"),
    ;

    private int code;
    private String message;

    CommonResultStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
