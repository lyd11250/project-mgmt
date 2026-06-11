package com.github.lyd11250.bedrock.system.storage;

/**
 * 存储完成后的对象描述：定位键 + 实际大小 + 内容指纹。
 *
 * @param objectKey   存储层对象键（如 MinIO 桶内相对路径），写入 {@code sys_file.object_key}
 * @param size        实际字节数
 * @param sha256      内容 SHA-256 十六进制串（可空）
 * @param storageType 存储后端标识（如 {@code minio}）
 */
public record StoredObject(String objectKey, long size, String sha256, String storageType) {
}
