package com.tiankong44.tool.apex.exception;

public class ApexException extends RuntimeException {
    private String errorCode;

    public ApexException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ApexException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}