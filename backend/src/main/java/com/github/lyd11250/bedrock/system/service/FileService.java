package com.github.lyd11250.bedrock.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.lyd11250.bedrock.common.BusinessException;
import com.github.lyd11250.bedrock.config.FileProperties;
import com.github.lyd11250.bedrock.system.QuotaKeys;
import com.github.lyd11250.bedrock.system.entity.SysFile;
import com.github.lyd11250.bedrock.system.mapper.SysFileMapper;
import com.github.lyd11250.bedrock.system.storage.StorageProvider;
import com.github.lyd11250.bedrock.system.storage.StoredObject;
import com.github.lyd11250.bedrock.system.vo.FileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;

/**
 * 文件上传下载（基座通用能力）。
 *
 * <p>元数据落 {@code sys_file}（多租户自动隔离），物理对象经 {@link StorageProvider} 存 MinIO。
 * 上传前用 {@link QuotaService} 校验租户存储用量配额（{@link QuotaKeys#MAX_STORAGE_BYTES}）。
 * 删除仅软删元数据，物理对象留待后续定时任务清理。
 */
@Service
@RequiredArgsConstructor
public class FileService {

    private final SysFileMapper fileMapper;
    private final StorageProvider storageProvider;
    private final QuotaService quotaService;
    private final FileProperties fileProperties;

    /** 下载载体：元数据 + 内容流（流由调用方负责关闭）。 */
    public record DownloadFile(String originalName, String contentType, long size, InputStream content) {
    }

    public FileVO upload(MultipartFile file, String bizType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        String ext = extOf(originalName);
        checkExtAllowed(ext);

        // 存储配额：已用量 + 本次大小不得超过套餐上限
        quotaService.checkAndAssert(QuotaKeys.MAX_STORAGE_BYTES, usedBytes() + file.getSize());

        StoredObject stored;
        try (InputStream in = file.getInputStream()) {
            stored = storageProvider.store(in, file.getSize(), file.getContentType(), null, ext);
        } catch (IOException e) {
            throw new BusinessException("读取上传文件失败");
        }

        SysFile entity = new SysFile();
        entity.setOriginalName(originalName);
        entity.setObjectKey(stored.objectKey());
        entity.setStorageType(stored.storageType());
        entity.setContentType(file.getContentType());
        entity.setSizeBytes(stored.size());
        entity.setSha256(stored.sha256());
        entity.setBizType(bizType);
        fileMapper.insert(entity);
        return toVO(entity);
    }

    public DownloadFile download(Long id) {
        SysFile entity = require(id);
        InputStream in = storageProvider.load(entity.getObjectKey());
        return new DownloadFile(entity.getOriginalName(), entity.getContentType(),
                entity.getSizeBytes() == null ? -1 : entity.getSizeBytes(), in);
    }

    public IPage<FileVO> page(long current, long size, String bizType) {
        IPage<SysFile> page = fileMapper.selectPage(Page.of(current, size),
                Wrappers.<SysFile>lambdaQuery()
                        .eq(bizType != null && !bizType.isBlank(), SysFile::getBizType, bizType)
                        .orderByDesc(SysFile::getId));
        return page.convert(this::toVO);
    }

    /** 软删元数据；物理对象保留，由后续定时任务按 object_key 清理。 */
    public void delete(Long id) {
        require(id);
        fileMapper.deleteById(id);
    }

    // ---- 内部 ----

    /** 当前租户已用存储字节数（租户过滤 + deleted=0 由插件自动追加）。 */
    private long usedBytes() {
        Map<String, Object> row = fileMapper.selectMaps(Wrappers.<SysFile>query()
                .select("COALESCE(SUM(size_bytes), 0) AS total")).stream().findFirst().orElse(null);
        if (row == null || row.get("total") == null) {
            return 0L;
        }
        return ((Number) row.get("total")).longValue();
    }

    private void checkExtAllowed(String ext) {
        var allowed = fileProperties.getAllowedExt();
        if (allowed == null || allowed.isEmpty()) {
            return;     // 未配置白名单 = 不限制
        }
        String bare = ext.startsWith(".") ? ext.substring(1) : ext;
        if (bare.isEmpty() || allowed.stream().noneMatch(e -> e.equalsIgnoreCase(bare))) {
            throw new BusinessException("不支持的文件类型：" + (bare.isEmpty() ? "(无扩展名)" : bare));
        }
    }

    /** 提取扩展名（含点，小写）；无扩展名返回空串。 */
    private String extOf(String name) {
        if (name == null) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (dot < 0 || dot < slash || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot).toLowerCase(Locale.ROOT);
    }

    private SysFile require(Long id) {
        SysFile entity = fileMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("文件不存在");
        }
        return entity;
    }

    private FileVO toVO(SysFile f) {
        FileVO v = new FileVO();
        v.setId(f.getId());
        v.setOriginalName(f.getOriginalName());
        v.setContentType(f.getContentType());
        v.setSizeBytes(f.getSizeBytes());
        v.setBizType(f.getBizType());
        v.setCreatedAt(f.getCreatedAt());
        return v;
    }
}
