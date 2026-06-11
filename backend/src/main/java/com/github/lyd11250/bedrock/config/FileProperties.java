package com.github.lyd11250.bedrock.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文件存储配置（前缀 {@code bedrock.file}）。
 *
 * <p>MinIO 端点/密钥/桶名按环境注入；{@code allowedExt} 为上传扩展名白名单（空 = 不限制）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "bedrock.file")
public class FileProperties {

    /** 允许上传的扩展名（小写，不含点）；为空表示不限制。 */
    private List<String> allowedExt;

    private final Minio minio = new Minio();

    @Data
    public static class Minio {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucket;
    }
}
