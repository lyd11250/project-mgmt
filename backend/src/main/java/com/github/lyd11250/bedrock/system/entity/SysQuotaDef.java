package com.github.lyd11250.bedrock.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.lyd11250.bedrock.common.GlobalBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 配额定义字典。全局表，不带 {@code tenant_id}。
 *
 * <p>{@code quotaKey} 全局唯一，{@code name} 为前端展示名称。基座内置 {@code max_users}，
 * biz 模块可自助新增定义（建议带模块前缀的 key，如 {@code biz:project:max_projects}）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_quota_def")
public class SysQuotaDef extends GlobalBaseEntity {

    /** 配额标识，如 max_users。 */
    private String quotaKey;

    /** 配额展示名称，如 最大用户数。 */
    private String name;

    private String remark;

    private Integer sort;
}
