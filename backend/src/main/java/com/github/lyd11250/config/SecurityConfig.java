package com.github.lyd11250.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 安全相关 Bean。此处仅使用 Spring Security 的 BCrypt 加密工具，
 * 不启用 Spring Security 框架本身（鉴权由 Sa-token 负责）。
 */
@Configuration
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
