package com.tiankong44.tool.reefjournal.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiankong44.tool.base.entity.BaseRes;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.tiankong44.tool.reefjournal.ReefJournalConstants.BEARER_PREFIX;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.HTTP_UNAUTHORIZED;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.REQUEST_USER_ID;

/**
 * 校验海缸接口的 bearer token 并写入请求级用户上下文。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Component
public class ReefAuthorizationInterceptor implements HandlerInterceptor {
    private final ReefJournalTokenService tokenService;
    private final ObjectMapper objectMapper;

    /**
     * 创建使用签名验证的海缸接口拦截器。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param tokenService 海缸令牌验证服务
     * @param objectMapper JSON 响应序列化器
     */
    public ReefAuthorizationInterceptor(ReefJournalTokenService tokenService, ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
    }

    /**
     * 未认证时返回统一登录提示，认证后只允许从请求属性取用户编号。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param request 当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param handler Spring MVC 处理器
     * @return 认证通过时继续执行请求
     * @throws IOException 写入统一错误响应失败时抛出
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return reject(response);
        }
        Long userId = tokenService.verify(authorization.substring(BEARER_PREFIX.length()).trim());
        if (userId == null) {
            return reject(response);
        }
        request.setAttribute(REQUEST_USER_ID, userId);
        return true;
    }

    /**
     * 返回一致的未登录错误响应，不泄露 token 校验细节。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param response 当前 HTTP 响应
     * @return 固定为 false，阻止后续控制器执行
     * @throws IOException JSON 响应写入失败时抛出
     */
    private boolean reject(HttpServletResponse response) throws IOException {
        response.setStatus(HTTP_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), BaseRes.failure("登录状态已过期，请重新进入小程序"));
        return false;
    }
}
