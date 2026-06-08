package com.github.lyd11250.config;

import java.util.function.Supplier;

/**
 * 租户上下文：在「无登录会话」或「需指定租户」的场景下临时覆盖多租户插件取到的租户 ID。
 *
 * <p>典型场景：登录时按租户编码定位用户、建租户时为其播种初始数据。
 * 取值优先级见 {@link TenantLineHandlerImpl#getTenantId()}：本上下文覆盖值 &gt; Sa-token 会话 &gt; 0。
 */
public final class TenantContext {

    private static final ThreadLocal<Long> OVERRIDE = new ThreadLocal<>();

    private TenantContext() {
    }

    /** 当前线程的租户覆盖值，无则返回 {@code null}。 */
    public static Long get() {
        return OVERRIDE.get();
    }

    /** 在指定租户上下文中执行（结束后自动还原），用于跨租户/登录前的数据操作。 */
    public static <T> T runAs(Long tenantId, Supplier<T> action) {
        Long previous = OVERRIDE.get();
        OVERRIDE.set(tenantId);
        try {
            return action.get();
        } finally {
            if (previous != null) {
                OVERRIDE.set(previous);
            } else {
                OVERRIDE.remove();
            }
        }
    }

    /** 在指定租户上下文中执行（无返回值）。 */
    public static void runAs(Long tenantId, Runnable action) {
        runAs(tenantId, () -> {
            action.run();
            return null;
        });
    }
}
