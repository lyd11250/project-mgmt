package com.github.lyd11250.bedrock.system.storage;

import java.io.InputStream;

/**
 * 文件存储抽象：屏蔽底层后端（MinIO / S3 / 本地磁盘 …）。
 *
 * <p>基座当前提供 MinIO 实现（{@link MinioStorageProvider}）。切换或新增后端只需另写一个实现，
 * 上传下载业务（{@code FileService}）无需改动。
 */
public interface StorageProvider {

    /**
     * 存储一个对象。
     *
     * @param in          数据流（由调用方负责关闭来源）
     * @param size        字节数（{@code <0} 表示未知）
     * @param contentType MIME 类型，可空
     * @param tenantId    租户 ID（用于物理隔离的对象键前缀）
     * @param ext         文件扩展名（含点，如 {@code .pdf}），可空
     * @return 存储结果（对象键 + 大小等）
     */
    StoredObject store(InputStream in, long size, String contentType, Long tenantId, String ext);

    /** 按对象键打开读取流（调用方负责关闭）。 */
    InputStream load(String objectKey);

    /** 删除对象（供后续定时清理软删文件使用）。 */
    void delete(String objectKey);
}
