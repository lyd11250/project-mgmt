package com.github.lyd11250.project.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.lyd11250.project.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    /** 角色码（租户内唯一），如 SUPER_ADMIN / TENANT_ADMIN / USER。 */
    private String code;

    private String name;
}
