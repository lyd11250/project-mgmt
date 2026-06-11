package com.github.lyd11250.bedrock.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-token 配置：注册登录校验拦截器，并放行开放接口。
 */
@Configuration
@RequiredArgsConstructor
public class SaTokenConfig implements WebMvcConfigurer {

    private final TenantStatusInterceptor tenantStatusInterceptor;

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
        // 1. 登录校验（先于租户状态校验，确保 StpUtil 会话已就绪）
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/**")
                .excludePathPatterns(EXCLUDE_PATHS);
        // 2. 租户状态校验（停用 / 订阅过期），同样放行开放接口
        registry.addInterceptor(tenantStatusInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(EXCLUDE_PATHS);
    }
}
