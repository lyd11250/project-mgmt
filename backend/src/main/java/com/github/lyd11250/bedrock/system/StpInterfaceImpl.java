package com.github.lyd11250.bedrock.system;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.lyd11250.bedrock.config.TenantLineHandlerImpl;
import com.github.lyd11250.bedrock.system.entity.SysMenu;
import com.github.lyd11250.bedrock.system.entity.SysPackageMenu;
import com.github.lyd11250.bedrock.system.entity.SysRole;
import com.github.lyd11250.bedrock.system.entity.SysRoleMenu;
import com.github.lyd11250.bedrock.system.entity.SysUserRole;
import com.github.lyd11250.bedrock.system.entity.Tenant;
import com.github.lyd11250.bedrock.system.mapper.SysMenuMapper;
import com.github.lyd11250.bedrock.system.mapper.SysPackageMenuMapper;
import com.github.lyd11250.bedrock.system.mapper.SysRoleMapper;
import com.github.lyd11250.bedrock.system.mapper.SysRoleMenuMapper;
import com.github.lyd11250.bedrock.system.mapper.SysUserRoleMapper;
import com.github.lyd11250.bedrock.system.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sa-token 鉴权数据源：按当前登录用户提供角色码与权限码。
 *
 * <p>权限码 = 角色分配的菜单 perm ∩ 租户套餐边界；超管（含 SUPER_ADMIN 角色）通配 {@code *}。
 * 查询时会话已写入 {@code tenantId}，{@code sys_role_menu} 由多租户插件自动按租户过滤；
 * {@code sys_menu / sys_package_menu / tenant} 为全局表，不参与过滤。
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;
    private final SysPackageMenuMapper packageMenuMapper;
    private final TenantMapper tenantMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        List<Long> roleIds = roleIdsOf(loginId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> roleCodes = roleCodesOf(roleIds);
        // 通配权限仅在平台租户内对 SUPER_ADMIN 生效；非平台租户即便存在该角色码也不授予 *（防越权提权）。
        if (roleCodes.contains(RbacConstants.ROLE_SUPER_ADMIN) && isPlatformTenant()) {
            return List.of(RbacConstants.PERMISSION_ALL);
        }

        // 角色分配的菜单 perm
        List<Long> assignedMenuIds = roleMenuMapper.selectList(
                        Wrappers.<SysRoleMenu>lambdaQuery().in(SysRoleMenu::getRoleId, roleIds))
                .stream().map(SysRoleMenu::getMenuId).distinct().toList();
        if (assignedMenuIds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> assignedPerms = permsOfMenus(assignedMenuIds);

        // 与套餐边界取交集
        assignedPerms.retainAll(boundaryPermsOfCurrentTenant());
        return List.copyOf(assignedPerms);
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        List<Long> roleIds = roleIdsOf(loginId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> roleCodes = roleCodesOf(roleIds);
        // 非平台租户内剔除 SUPER_ADMIN，确保 @SaCheckRole("SUPER_ADMIN") 守卫的平台级接口无法被越权命中。
        if (!isPlatformTenant()) {
            return roleCodes.stream()
                    .filter(code -> !RbacConstants.ROLE_SUPER_ADMIN.equals(code))
                    .toList();
        }
        return roleCodes;
    }

    // ---- 内部 ----

    /** 当前会话租户是否为平台租户。 */
    private boolean isPlatformTenant() {
        Object tid = StpUtil.getSession().get(TenantLineHandlerImpl.SESSION_TENANT_ID);
        return tid != null && RbacConstants.PLATFORM_TENANT_ID.equals(Long.valueOf(tid.toString()));
    }

    private List<Long> roleIdsOf(Object loginId) {
        Long userId = Long.valueOf(loginId.toString());
        return userRoleMapper.selectList(
                        Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).distinct().toList();
    }

    private List<String> roleCodesOf(List<Long> roleIds) {
        return roleMapper.selectList(Wrappers.<SysRole>lambdaQuery().in(SysRole::getId, roleIds))
                .stream().map(SysRole::getCode).distinct().toList();
    }

    /** 菜单 id 集合 → 非空 perm 集合。 */
    private Set<String> permsOfMenus(List<Long> menuIds) {
        if (menuIds.isEmpty()) {
            return Collections.emptySet();
        }
        return menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery().in(SysMenu::getId, menuIds))
                .stream().map(SysMenu::getPerm).filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    /** 当前租户套餐圈定的 perm 集合（package_id 为空则为空集）。 */
    private Set<String> boundaryPermsOfCurrentTenant() {
        Object tid = StpUtil.getSession().get(TenantLineHandlerImpl.SESSION_TENANT_ID);
        if (tid == null) {
            return Collections.emptySet();
        }
        Tenant tenant = tenantMapper.selectById(Long.valueOf(tid.toString()));
        if (tenant == null || tenant.getPackageId() == null) {
            return Collections.emptySet();
        }
        List<Long> packageMenuIds = packageMenuMapper.selectList(
                        Wrappers.<SysPackageMenu>lambdaQuery().eq(SysPackageMenu::getPackageId, tenant.getPackageId()))
                .stream().map(SysPackageMenu::getMenuId).distinct().toList();
        return permsOfMenus(packageMenuIds);
    }
}
