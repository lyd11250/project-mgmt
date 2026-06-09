package com.github.lyd11250.bedrock.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 套餐创建/编辑入参（平台超管）。
 */
@Data
public class PackageDTO {

    @NotBlank(message = "套餐名称不能为空")
    private String name;

    @NotBlank(message = "套餐编码不能为空")
    private String code;

    private Integer status;

    private String remark;

    /**
     * 配额（键值模型）：quota_key → 上限值，-1 表示不限。
     * 传入即全量 upsert；新增配额项无需改基座代码（biz 模块可自带 key）。
     */
    private Map<String, Long> quotas;
}
