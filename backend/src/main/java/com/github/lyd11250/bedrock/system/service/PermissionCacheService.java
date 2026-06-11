package com.github.lyd11250.bedrock.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.lyd11250.bedrock.config.CacheConfig;
import com.github.lyd11250.bedrock.config.TenantLineHandlerImpl;
import com.github.lyd11250.bedrock.system.RbacConstants;
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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 鉴权数据缓存：把「用户权限码集合 / 角色码集合」缓存到 Redis，规避每次鉴权 5~6 条查库（G10）。
 *
 * <p>计算逻辑由 {@code StpInterfaceImpl} 迁入此处，使其可被 Spring Cache 拦截：
 * 缓存 key = {@code tenantId:userId}，由所属租户与用户唯一确定。命中即直接返回，未命中才查库并回填。
 *
 * <p><b>失效按粒度精确进行</b>，避免一刀切清空波及无关租户：
 * <ul>
 *   <li>用户级（用户角色分配/删除）→ {@link #evictUser}，仅清该用户两条 key；</li>
 *   <li>租户级（角色菜单分配/删除、租户换套餐）→ {@link #evictTenant}，按 {@code tenantId:*} 前缀清该租户；</li>
 *   <li>套餐级（套餐菜单变更/删除）→ {@link #evictByPackage}，仅清订阅该套餐的那些租户；</li>
 *   <li>全局（全局菜单 perm 变更，本就影响所有租户）→ {@link #evictAll}。</li>
 * </ul>
 * 10 分钟 TTL 再兜底一层「漏失效」。
 */
@Service
@RequiredArgsConstructor
public class PermissionCacheService {

    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;
    private final SysPackageMenuMapper packageMenuMapper;
    private final TenantMapper tenantMapper;
    private final StringRedisTemplate redisTemplate;

    /** 用户在指定租户下的权限码集合（含套餐边界裁剪；平台超管为通配 {@code *}）。 */
    @Cacheable(cacheNames = CacheConfig.CACHE_RBAC_PERM, key = "#tenantId + ':' + #userId")
    public List<String> permissions(long tenantId, long userId) {
        List<Long> roleIds = roleIdsOf(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> roleCodes = roleCodesOf(roleIds);
        // 通配权限仅在平台租户内对 SUPER_ADMIN 生效；非平台租户即便存在该角色码也不授予 *（防越权提权）。
        if (roleCodes.contains(RbacConstants.ROLE_SUPER_ADMIN) && isPlatformTenant(tenantId)) {
            return List.of(RbacConstants.PERMISSION_ALL);
        }
        // 租户管理员：动态拥有「本租户套餐边界内全部菜单」，不依赖 sys_role_menu 快照——
        // 套餐扩容（给套餐加菜单）即时对存量租户管理员生效，无需重新分配菜单。
        // 普通用户 / 自建角色仍走显式分配（assignedPerms ∩ boundary）。
        if (roleCodes.contains(RbacConstants.ROLE_TENANT_ADMIN)) {
            return List.copyOf(boundaryPermsOfTenant(tenantId));
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
        assignedPerms.retainAll(boundaryPermsOfTenant(tenantId));
        return List.copyOf(assignedPerms);
    }

    /** 用户在指定租户下的角色码集合（非平台租户内剔除 SUPER_ADMIN，防越权命中平台级接口）。 */
    @Cacheable(cacheNames = CacheConfig.CACHE_RBAC_ROLE, key = "#tenantId + ':' + #userId")
    public List<String> roles(long tenantId, long userId) {
        List<Long> roleIds = roleIdsOf(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> roleCodes = roleCodesOf(roleIds);
        if (!isPlatformTenant(tenantId)) {
            return roleCodes.stream()
                    .filter(code -> !RbacConstants.ROLE_SUPER_ADMIN.equals(code))
                    .toList();
        }
        return roleCodes;
    }

    // ---- 失效（按粒度精确，仅清相关 key） ----

    /** 清单个用户在指定租户下的权限/角色缓存。 */
    public void evictUser(long tenantId, long userId) {
        String suffix = tenantId + ":" + userId;
        redisTemplate.delete(List.of(
                fullKey(CacheConfig.CACHE_RBAC_PERM, suffix),
                fullKey(CacheConfig.CACHE_RBAC_ROLE, suffix)));
    }

    /** 清单个用户（当前会话租户）的缓存。供租户内用户角色变更调用。 */
    public void evictUserInCurrentTenant(long userId) {
        evictUser(currentTenantId(), userId);
    }

    /** 清指定租户下所有用户的缓存（按 {@code tenantId:*} 前缀 SCAN 删除）。 */
    public void evictTenant(long tenantId) {
        deleteByPattern(fullKey(CacheConfig.CACHE_RBAC_PERM, tenantId + ":*"));
        deleteByPattern(fullKey(CacheConfig.CACHE_RBAC_ROLE, tenantId + ":*"));
    }

    /** 清当前会话租户下所有用户的缓存。供租户内角色/菜单变更调用。 */
    public void evictCurrentTenant() {
        evictTenant(currentTenantId());
    }

    /** 清订阅指定套餐的所有租户缓存（套餐边界变更影响这些租户的全体用户）。 */
    public void evictByPackage(long packageId) {
        tenantMapper.selectList(Wrappers.<Tenant>lambdaQuery().eq(Tenant::getPackageId, packageId))
                .forEach(t -> evictTenant(t.getId()));
    }

    /** 清空全部鉴权缓存。仅供「全局菜单 perm 变更」这类波及所有租户的场景调用。 */
    public void evictAll() {
        deleteByPattern(fullKey(CacheConfig.CACHE_RBAC_PERM, "*"));
        deleteByPattern(fullKey(CacheConfig.CACHE_RBAC_ROLE, "*"));
    }

    // ---- 内部 ----

    /** RedisCacheManager 实际写入的完整 key：前缀 + 缓存名 + ":" + 业务 key。 */
    private String fullKey(String cacheName, String suffix) {
        return CacheConfig.KEY_PREFIX + cacheName + ":" + suffix;
    }

    private void deleteByPattern(String pattern) {
        List<String> keys = new ArrayList<>();
        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match(pattern).count(256).build())) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        }
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private long currentTenantId() {
        Object tid = StpUtil.getSession().get(TenantLineHandlerImpl.SESSION_TENANT_ID);
        return tid == null ? 0L : Long.parseLong(tid.toString());
    }

    private boolean isPlatformTenant(long tenantId) {
        return RbacConstants.PLATFORM_TENANT_ID.equals(tenantId);
    }

    private List<Long> roleIdsOf(long userId) {
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

    /** 租户套餐圈定的 perm 集合（package_id 为空则为空集）。 */
    private Set<String> boundaryPermsOfTenant(long tenantId) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null || tenant.getPackageId() == null) {
            return Collections.emptySet();
        }
        List<Long> packageMenuIds = packageMenuMapper.selectList(
                        Wrappers.<SysPackageMenu>lambdaQuery().eq(SysPackageMenu::getPackageId, tenant.getPackageId()))
                .stream().map(SysPackageMenu::getMenuId).distinct().toList();
        return permsOfMenus(packageMenuIds);
    }
}
