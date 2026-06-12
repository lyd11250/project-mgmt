package com.github.lyd11250.bedrock.system.service;

import com.github.lyd11250.bedrock.system.spi.FileBizTypeDef;
import com.github.lyd11250.bedrock.system.spi.FileBizTypeProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文件业务类型注册表：聚合各模块 {@link FileBizTypeProvider} 的登记，提供 {@code value → 中文标签} 翻译。
 *
 * <p>仅做标签翻译；「下拉里出现哪些 bizType」由 {@link FileService} 按当前租户实有数据裁剪，
 * 故全局注册表不会把租户套餐外的业务类型暴露出去。
 */
@Service
public class FileBizTypeRegistry {

    private final Map<String, String> labels;

    public FileBizTypeRegistry(List<FileBizTypeProvider> providers) {
        this.labels = providers.stream()
                .flatMap(p -> p.bizTypes().stream())
                .collect(Collectors.toMap(FileBizTypeDef::value, FileBizTypeDef::label, (a, b) -> a));
    }

    /** 翻译为中文标签；未登记的值原样返回（兜底）。 */
    public String labelOf(String value) {
        return labels.getOrDefault(value, value);
    }
}
