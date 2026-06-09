package com.github.lyd11250.bedrock.system;

/**
 * RBAC 常量：角色码、套餐、通配权限。
 *
 * <p>权限码不再硬编码于此——已迁移到 DB 全局菜单 {@code sys_menu.perm}（见 Flyway V2）。
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

    /**
     * 系统保留角色码：禁止租户经接口自行创建/复用。
     * 尤其 {@code SUPER_ADMIN} 关联通配权限与跨租户能力，放开将导致越权提权。
     * （播种逻辑 {@code SeedService} 直接落库，不走此校验。）
     */
    public static final java.util.Set<String> RESERVED_ROLE_CODES =
            java.util.Set.of(ROLE_SUPER_ADMIN, ROLE_TENANT_ADMIN, ROLE_USER);

    /** 判断角色码是否为系统保留（忽略大小写与首尾空白）。 */
    public static boolean isReservedRoleCode(String code) {
        if (code == null) {
            return false;
        }
        String trimmed = code.trim();
        return RESERVED_ROLE_CODES.stream().anyMatch(r -> r.equalsIgnoreCase(trimmed));
    }

    /** 通配权限（超级管理员，绕过套餐边界与菜单分配）。 */
    public static final String PERMISSION_ALL = "*";

    // 内置套餐固定 id（见 Flyway V2 种子）
    /** 基础版：用户管理 + 角色管理。新建租户默认套餐。 */
    public static final Long PACKAGE_BASIC_ID = 1L;
    /** 全功能：全部系统菜单。平台租户套餐。 */
    public static final Long PACKAGE_FULL_ID = 2L;
}
