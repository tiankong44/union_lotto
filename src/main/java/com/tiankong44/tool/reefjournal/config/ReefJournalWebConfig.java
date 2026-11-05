package com.tiankong44.tool.reefjournal.config;

import com.tiankong44.tool.reefjournal.auth.ReefAuthorizationInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册海缸模块认证拦截器与外部化配置。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Configuration
@EnableConfigurationProperties(ReefJournalProperties.class)
public class ReefJournalWebConfig implements WebMvcConfigurer {
    private final ReefAuthorizationInterceptor authorizationInterceptor;

    /**
     * 创建海缸接口配置并注入请求认证拦截器。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param authorizationInterceptor 海缸接口认证拦截器
     */
    public ReefJournalWebConfig(ReefAuthorizationInterceptor authorizationInterceptor) {
        this.authorizationInterceptor = authorizationInterceptor;
    }

    /**
     * 只保护海缸日记接口，微信登录入口保持公开。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param registry Spring MVC 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 将身份校验限制在新模块，不改变其他既有接口的访问方式。
        registry.addInterceptor(authorizationInterceptor)
                .addPathPatterns("/reef-journal/**")
                .excludePathPatterns("/reef-journal/auth/login", "/reef-journal/photos/*/content");
    }
}
