package com.github.lyd11250.bedrock.system.vo;

import lombok.Data;

/**
 * 套餐配额视图对象（配额定义 + 该套餐配置值）。
 *
 * <p>{@code quotaValue = -1} 表示不限（含未配置）。
 */
@Data
public class PackageQuotaVO {

    /** 配额定义 id（sys_quota_def.id）。 */
    private Long quotaId;

    private String quotaKey;

    /** 配额展示名称，如 最大用户数。 */
    private String quotaName;

    private String remark;

    /** 该套餐的配额上限，-1 表示不限。 */
    private Long quotaValue;
}
