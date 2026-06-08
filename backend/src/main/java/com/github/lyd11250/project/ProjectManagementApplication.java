package com.github.lyd11250.project;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用入口。
 *
 * <p>多租户项目协作平台后端 —— Spring Boot 4 + MyBatis-Plus + Sa-token + PostgreSQL + Redis。
 */
@SpringBootApplication
@MapperScan("com.github.lyd11250.project.**.mapper")
public class ProjectManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjectManagementApplication.class, args);
    }
}
