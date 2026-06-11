package com.github.lyd11250.bedrock.config;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 客户端配置。
 *
 * <p>Spring Boot 4 自动配置已模块化，第三方库不再触发自动装配，故在此手动声明
 * {@link MinioClient} Bean（不依赖任何 minio starter）。桶的存在性在
 * {@code MinioStorageProvider} 初始化时按需创建。
 */
@Configuration
@RequiredArgsConstructor
public class MinioConfig {

    private final FileProperties fileProperties;

    @Bean
    public MinioClient minioClient() {
        FileProperties.Minio minio = fileProperties.getMinio();
        return MinioClient.builder()
                .endpoint(minio.getEndpoint())
                .credentials(minio.getAccessKey(), minio.getSecretKey())
                .build();
    }
}
