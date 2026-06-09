package com.github.lyd11250.bedrock.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.lyd11250.bedrock.common.GlobalBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 套餐配额（键值模型）。全局表，不带 {@code tenant_id}。
 *
 * <p>{@code quotaValue = -1} 表示不限；无记录视为缺省放行（见 {@code QuotaService}）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_package_quota")
public class SysPackageQuota extends GlobalBaseEntity {

    private Long packageId;

    /** 配额标识，如 max_users。 */
    private String quotaKey;

    /** 配额上限，-1 表示不限。 */
    private Long quotaValue;
}
