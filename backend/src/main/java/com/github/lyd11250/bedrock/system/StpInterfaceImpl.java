package com.github.lyd11250.bedrock.system;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.github.lyd11250.bedrock.config.TenantLineHandlerImpl;
import com.github.lyd11250.bedrock.system.service.PermissionCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sa-token 鉴权数据源：按当前登录用户提供角色码与权限码。
 *
 * <p>仅负责从会话解析 {@code tenantId} 与 {@code userId}，实际查库与裁剪逻辑下沉到
 * {@link PermissionCacheService}（带 Redis 缓存，规避每次鉴权重复查库，见 G10）。
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final PermissionCacheService permissionCache;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return permissionCache.permissions(currentTenantId(), Long.parseLong(loginId.toString()));
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return permissionCache.roles(currentTenantId(), Long.parseLong(loginId.toString()));
    }

    /** 当前会话租户 ID，缺省回退系统租户 0。 */
    private long currentTenantId() {
        Object tid = StpUtil.getSession().get(TenantLineHandlerImpl.SESSION_TENANT_ID);
        return tid == null ? 0L : Long.parseLong(tid.toString());
    }
}
