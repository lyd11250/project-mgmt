package com.github.lyd11250.bedrock.system.storage;

import com.github.lyd11250.bedrock.common.BusinessException;
import com.github.lyd11250.bedrock.config.FileProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.UUID;

/**
 * MinIO 存储实现。
 *
 * <p>对象键规约 {@code {tenantId}/{yyyyMM}/{uuid}{ext}}：租户前缀做物理隔离，按月分桶避免单目录过大。
 * 上传时用 {@link DigestInputStream} 单遍计算 SHA-256，无需二次读流。桶不存在则启动时创建。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinioStorageProvider implements StorageProvider {

    private static final String STORAGE_TYPE = "minio";
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyyMM");
    /** size 未知时的分片大小（MinIO 要求 ≥ 5MiB）。 */
    private static final long DEFAULT_PART_SIZE = 10L * 1024 * 1024;

    private final MinioClient minioClient;
    private final FileProperties fileProperties;

    private String bucket;

    @PostConstruct
    public void init() {
        this.bucket = fileProperties.getMinio().getBucket();
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO 桶不存在，已创建：{}", bucket);
            }
        } catch (Exception e) {
            throw new IllegalStateException("初始化 MinIO 桶失败：" + bucket, e);
        }
    }

    @Override
    public StoredObject store(InputStream in, long size, String contentType, Long tenantId, String ext) {
        String objectKey = (tenantId == null ? 0L : tenantId)
                + "/" + LocalDate.now().format(MONTH)
                + "/" + UUID.randomUUID().toString().replace("-", "")
                + (ext == null ? "" : ext);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            DigestInputStream dis = new DigestInputStream(in, digest);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(dis, size, size >= 0 ? -1 : DEFAULT_PART_SIZE)
                    .contentType(contentType == null ? "application/octet-stream" : contentType)
                    .build());
            String sha256 = HexFormat.of().formatHex(digest.digest());
            return new StoredObject(objectKey, size, sha256, STORAGE_TYPE);
        } catch (Exception e) {
            log.error("上传对象到 MinIO 失败：{}", objectKey, e);
            throw new BusinessException("文件上传失败");
        }
    }

    @Override
    public InputStream load(String objectKey) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            log.error("从 MinIO 读取对象失败：{}", objectKey, e);
            throw new BusinessException("文件读取失败");
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            log.error("从 MinIO 删除对象失败：{}", objectKey, e);
            throw new BusinessException("文件删除失败");
        }
    }
}
