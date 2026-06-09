package com.github.lyd11250.bedrock.system;

import java.util.List;
import java.util.Map;

/**
 * RBAC 常量：角色码、权限码与默认绑定。
 */
public final class RbacConstants {

    private RbacConstants() {
    }

    /** 会话登录类型（Sa-token 默认）。 */
    public static final String LOGIN_TYPE = "login";

    /** 平台租户（隔离根之上的运维租户）固定 ID 与编码。 */
    public static final Long PLATFORM_TENANT_ID = 1L;
    public static final String PLATFORM_TENANT_CODE = "PLATFORM";

    // 角色码
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_TENANT_ADMIN = "TENANT_ADMIN";
    public static final String ROLE_USER = "USER";

    /** 通配权限（超级管理员）。 */
    public static final String PERMISSION_ALL = "*";

    /** 租户内权限目录：code → 名称。 */
    public static final Map<String, String> TENANT_PERMISSIONS = Map.ofEntries(
            Map.entry("user:list", "用户查看"),
            Map.entry("user:create", "用户创建"),
            Map.entry("user:update", "用户编辑"),
            Map.entry("user:delete", "用户删除"),
            Map.entry("user:resetPwd", "重置密码"),
            Map.entry("user:assignRole", "分配角色"),
            Map.entry("role:list", "角色查看")
    );

    /** 普通用户默认拥有的权限码（本期仅只读查看）。 */
    public static final List<String> USER_PERMISSIONS = List.of("user:list", "role:list");
}
