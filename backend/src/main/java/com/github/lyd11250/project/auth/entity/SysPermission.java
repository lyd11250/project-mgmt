package com.github.lyd11250.project.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.lyd11250.project.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 权限。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class SysPermission extends BaseEntity {

    /** 权限码（租户内唯一），如 user:create；{@code *} 表示通配（超级管理员）。 */
    private String code;

    private String name;

    /** 权限类型（如 menu / button / api），本期暂不细分。 */
    private String type;
}
