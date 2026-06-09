package com.github.lyd11250.bedrock;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用入口。
 *
 * <p>Bedrock —— 多租户 SaaS 平台基座。Spring Boot 4 + MyBatis-Plus + Sa-token + PostgreSQL + Redis。
 */
@SpringBootApplication
@MapperScan("com.github.lyd11250.bedrock.**.mapper")
public class BedrockApplication {

    public static void main(String[] args) {
        SpringApplication.run(BedrockApplication.class, args);
    }
}
