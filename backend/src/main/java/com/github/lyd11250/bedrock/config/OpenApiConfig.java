package com.github.lyd11250.bedrock.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * springdoc-openapi 配置：API 元信息 + Sa-token 令牌鉴权方案。
 *
 * <p>Swagger UI 默认地址 {@code /swagger-ui.html}，OpenAPI JSON 为 {@code /v3/api-docs}（已在
 * {@link SaTokenConfig} 放行）。鉴权方案声明为 apiKey + header（头名取 {@code sa-token.token-name}），
 * 在 Swagger UI 点「Authorize」填入登录返回的令牌即可调试需登录接口。
 */
@Configuration
public class OpenApiConfig {

    /** 与 application.yml 的 {@code sa-token.token-name} 保持一致。 */
    @Value("${sa-token.token-name:satoken}")
    private String tokenName;

    @Bean
    public OpenAPI bedrockOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bedrock 多租户 SaaS 平台基座 API")
                        .description("系统能力域接口（租户/套餐/RBAC/菜单/鉴权）。统一前缀 /api/v1，响应体 {code,message,data}。")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(tokenName, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(tokenName)))
                .addSecurityItem(new SecurityRequirement().addList(tokenName));
    }
}
