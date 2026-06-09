package com.github.lyd11250.bedrock.system;

/**
 * 套餐配额标识（{@code sys_package_quota.quota_key}）。
 *
 * <p>基座内置 {@link #MAX_USERS}；上层 biz 模块可自定义 key（建议带模块前缀，如
 * {@code biz:project:max_projects}），无需改基座——在套餐管理页或自身 Flyway 种子配置值，
 * 并在写操作处调用 {@code QuotaService} 校验即可。
 */
public final class QuotaKeys {

    private QuotaKeys() {
    }

    /** 单租户最大用户数。 */
    public static final String MAX_USERS = "max_users";

    /** 配额未配置/不限的约定值。 */
    public static final long UNLIMITED = -1L;
}
