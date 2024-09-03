package com.tiankong44.tool.aop;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tiankong44.tool.base.entity.BaseRes;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpUtils;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @Description :日志切面，记录接口出参入参
 * @Author zhanghao_SMEICS
 * @Date 2022/10/21  16:06
 **/
@Aspect
@Component
@Slf4j
public class LogAspect {

    @Pointcut("@annotation(org.springframework.web.bind.annotation.RequestMapping) ||" + " @annotation(org.springframework.web.bind.annotation.GetMapping) || " + "@annotation(org.springframework.web.bind.annotation.PostMapping) || " + "@annotation(org.springframework.web.bind.annotation.DeleteMapping)|| " + "@annotation(org.springframework.web.bind.annotation.PutMapping)")
    public void controllerReqCut() {
    }

    @Around("controllerReqCut()")
    public Object interceptorReq(ProceedingJoinPoint joinPoint) throws Throwable {
        String traceId = RandomUtil.randomNumbers(6);
        MDC.put("traceId", traceId);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        // 获取当前的HttpServletRequest
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        String url = request.getRequestURI();
        log.info("请求地址: {}", url);
        Signature signature = joinPoint.getSignature();//获取连接点的方法签名对象
        MethodSignature methodSignature = null;
        if (!(signature instanceof MethodSignature)) {
            throw new IllegalArgumentException("该注解只能用于方法");
        }
        methodSignature = (MethodSignature) signature;
        Object o = joinPoint.getTarget();//获取连接点所在的目标对象
        Object[] args = joinPoint.getArgs();//获取参数
        Method currentMethod = methodSignature.getMethod();
        String funcName = o.getClass() + "." + currentMethod.getName();
        StringBuilder bf = new StringBuilder(funcName + "方法入参：");
        for (int i = 0; i < args.length; i++) {
            String jsonString = JSONObject.toJSONString(args[i]);
            bf.append(jsonString);

        }

        log.info(bf.toString());
        Object retObj = null;
        try {
            retObj = joinPoint.proceed(args);
            //通过反射的方法执行切面切到的方法实体,如果有传参则不使用原来的参数进行方法,如果不调用此方法，那被切的面后面的代码不会被执行
        } finally {
            String retStr = funcName + "方法回参：" + JSON.toJSON(retObj);
            log.info(retStr);
            stopWatch.stop();
            log.info(funcName + "接口执行时间:{}ms", stopWatch.getTotalTimeMillis());
            // MDC.remove(Constant.LOG_TRACE_ID);
        }
        return retObj;
    }
}
