package com.github.lyd11250.bedrock.system.vo;

import lombok.Data;

import java.util.Map;

/**
 * 套餐视图对象。
 */
@Data
public class PackageVO {

    private Long id;

    private String name;

    private String code;

    private Integer status;

    private String remark;

    /** 配额（键值模型）：quota_key → 上限值，-1 表示不限。 */
    private Map<String, Long> quotas;
}
