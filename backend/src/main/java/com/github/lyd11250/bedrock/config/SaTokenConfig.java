package com.github.lyd11250.bedrock.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-token 配置：注册登录校验拦截器，并放行开放接口。
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    /** 无需登录即可访问的路径。 */
    private static final String[] EXCLUDE_PATHS = {
            "/api/v1/ping",
            "/api/v1/system/auth/login",
            // API 文档（springdoc）：UI、静态资源与 OpenAPI JSON
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**",
            "/error"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/**")
                .excludePathPatterns(EXCLUDE_PATHS);
    }
}
