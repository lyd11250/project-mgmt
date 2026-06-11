package com.github.lyd11250.bedrock.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件信息出参。{@code objectKey} 等存储细节不外泄，下载走 {@code /api/v1/system/files/{id}}。
 */
@Data
public class FileVO {

    private Long id;

    private String originalName;

    private String contentType;

    private Long sizeBytes;

    private String bizType;

    private LocalDateTime createdAt;
}
