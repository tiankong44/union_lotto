package com.tiankong44.tool.reefjournal.exception;

import com.tiankong44.tool.base.entity.BaseRes;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 将海缸模块业务失败转换为现有统一响应结构。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@RestControllerAdvice(basePackages = "com.tiankong44.tool.reefjournal")
public class ReefJournalExceptionHandler {
    /**
     * 返回面向小程序的业务错误，不暴露密钥或远端响应正文。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param exception 海缸模块业务异常
     * @return 现有 BaseRes 业务失败结构
     */
    @ExceptionHandler(ReefJournalException.class)
    public BaseRes<Void> handleBusinessException(ReefJournalException exception) {
        return BaseRes.failure(exception.getMessage());
    }
}
