package com.tiankong44.tool.apex.exception;

import com.tiankong44.tool.base.entity.BaseRes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
@ResponseBody
public class ApexExceptionHandler {

    @ExceptionHandler(ApexException.class)
    public BaseRes handleApexException(ApexException e) {
        log.error("Apex exception occurred: ", e);
        
        Map<String, String> error = new HashMap<>();
        error.put("code", e.getErrorCode());
        error.put("message", e.getMessage());
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", error);
        
        return BaseRes.builder()
                .code(1)
                .message("Operation failed")
                .data(result)
                .build();
    }
}