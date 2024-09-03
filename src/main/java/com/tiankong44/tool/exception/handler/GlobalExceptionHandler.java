package com.tiankong44.tool.exception.handler;

import com.alibaba.fastjson.JSONObject;
import com.tiankong44.tool.base.entity.BaseRes;
import com.tiankong44.tool.exception.customException.DifferentCoordinateException;
import com.tiankong44.tool.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.ConstraintViolationException;

/**
 * @author zhanghao_SMEICS
 * @date 2022-10-21 22:39
 */
@ControllerAdvice
@Slf4j
@ResponseBody
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseRes handleValidationException(MethodArgumentNotValidException ex) {
        StringBuilder stringBuilder = new StringBuilder();
        ex.getBindingResult().getAllErrors().forEach(
                error -> {
                    String fieldName = ((FieldError) error).getField();
                    String errorMessage = error.getDefaultMessage();
                    stringBuilder.append(fieldName).append(":").append(errorMessage).append(",");

                }
        );

        String message = stringBuilder.toString();
        message = StringUtil.trimEnd(message, ",");

        BaseRes failure = BaseRes.failure(message);
        log.info("方法回参:"+ JSONObject.toJSONString(failure));
        return failure;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public BaseRes handleConstraintViolationException(ConstraintViolationException e) {
        e.printStackTrace();
        String message = e.getMessage();

        BaseRes failure = BaseRes.failure(message);
        log.info("方法回参:"+ JSONObject.toJSONString(failure));
        return failure;
    }

    @ExceptionHandler(DifferentCoordinateException.class)
    public BaseRes handleDifferentCoordinateExceptio(DifferentCoordinateException e) {
        e.printStackTrace();
        String message = e.getMessage();

        BaseRes failure = BaseRes.failure(message);
        log.info(e.getLocation()+"方法回参:"+ JSONObject.toJSONString(failure));
        return failure;
    }

    @ExceptionHandler(NullPointerException.class)
    public BaseRes handleNullPointerException(NullPointerException e) {
        e.printStackTrace();
        String message = e.getMessage();

        BaseRes failure = BaseRes.failure(message);
        log.info("方法回参:"+ JSONObject.toJSONString(failure));
        return failure;
    }

    @ExceptionHandler(Exception.class)
    public BaseRes handleException(Exception e) {
        e.printStackTrace();
        String message = e.getMessage();
        BaseRes failure = BaseRes.failure(message);
        log.info("方法回参:"+ JSONObject.toJSONString(failure));
        return failure;
    }
}
