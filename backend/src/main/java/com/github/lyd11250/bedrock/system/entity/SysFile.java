package com.github.lyd11250.bedrock.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.lyd11250.bedrock.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件元数据（业务表，多租户隔离；物理对象存 MinIO）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_file")
public class SysFile extends BaseEntity {

    /** 原始文件名（展示/下载用）。 */
    private String originalName;

    /** 存储层对象键（MinIO 桶内相对路径）。 */
    private String objectKey;

    /** 存储后端标识，如 minio。 */
    private String storageType;

    /** MIME 类型。 */
    private String contentType;

    /** 字节数。 */
    private Long sizeBytes;

    /** 内容 SHA-256 指纹。 */
    private String sha256;

    /** 业务归类（上层模块自定义，如 party:attachment）。 */
    private String bizType;
}
