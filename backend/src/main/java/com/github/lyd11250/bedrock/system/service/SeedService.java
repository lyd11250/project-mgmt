package com.github.lyd11250.bedrock.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.lyd11250.bedrock.system.RbacConstants;
import com.github.lyd11250.bedrock.system.entity.SysMenu;
import com.github.lyd11250.bedrock.system.entity.SysPackageMenu;
import com.github.lyd11250.bedrock.system.entity.SysRole;
import com.github.lyd11250.bedrock.system.entity.SysRoleMenu;
import com.github.lyd11250.bedrock.system.entity.SysUser;
import com.github.lyd11250.bedrock.system.entity.SysUserRole;
import com.github.lyd11250.bedrock.system.mapper.SysMenuMapper;
import com.github.lyd11250.bedrock.system.mapper.SysPackageMenuMapper;
import com.github.lyd11250.bedrock.system.mapper.SysRoleMapper;
import com.github.lyd11250.bedrock.system.mapper.SysRoleMenuMapper;
import com.github.lyd11250.bedrock.system.mapper.SysUserMapper;
import com.github.lyd11250.bedrock.system.mapper.SysUserRoleMapper;
import com.github.lyd11250.bedrock.config.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 种子/初始化逻辑：为「平台租户」与「新建租户」播种角色与管理员。
 *
 * <p>角色权限来自「角色↔菜单分配」（{@code sys_role_menu}），菜单范围受租户套餐圈定；
 * 不再每租户复制权限目录。所有写入均在 {@link TenantContext#runAs} 指定的租户上下文中执行。
 */
@Service
@RequiredArgsConstructor
public class SeedService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysPackageMenuMapper packageMenuMapper;
    private final SysMenuMapper menuMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * 为平台租户播种：SUPER_ADMIN 角色（通配权限，无需绑菜单）+ 超管账号。
     */
    @Transactional
    public void seedPlatform(Long platformTenantId, String adminUsername, String adminRawPassword) {
        TenantContext.runAs(platformTenantId, () -> {
            Long superRoleId = createRole(RbacConstants.ROLE_SUPER_ADMIN, "超级管理员");
            Long userId = createUser(adminUsername, adminRawPassword);
            bindUserRole(userId, superRoleId);
            return null;
        });
    }

    /**
     * 为新建租户播种：TENANT_ADMIN（套餐内全部菜单）/ USER（套餐内只读页面）角色 + 租户管理员账号。
     */
    @Transactional
    public void seedTenant(Long tenantId, Long packageId, String adminUsername, String adminRawPassword) {
        TenantContext.runAs(tenantId, () -> {
            List<Long> packageMenuIds = menuIdsOfPackage(packageId);

            // 租户管理员：权限动态 =「套餐边界内全部菜单」（见 PermissionCacheService.permissions），
            // 不再快照绑定 sys_role_menu，故套餐后续扩容对存量租户管理员自动生效，无需重新分配。
            Long adminRoleId = createRole(RbacConstants.ROLE_TENANT_ADMIN, "租户管理员");
            // 普通用户：仅套餐内 C 型页面（只读），仍走显式分配（自建角色同理）
            Long userRoleId = createRole(RbacConstants.ROLE_USER, "普通用户");
            for (Long menuId : pageMenuIds(packageMenuIds)) {
                bindRoleMenu(userRoleId, menuId);
            }
            // 租户管理员账号
            Long uid = createUser(adminUsername, adminRawPassword);
            bindUserRole(uid, adminRoleId);
            return null;
        });
    }

    // ---- 细粒度构建 ----

    public Long createRole(String code, String name) {
        SysRole role = new SysRole();
        role.setCode(code);
        role.setName(name);
        roleMapper.insert(role);
        return role.getId();
    }

    public void bindRoleMenu(Long roleId, Long menuId) {
        SysRoleMenu rm = new SysRoleMenu();
        rm.setRoleId(roleId);
        rm.setMenuId(menuId);
        roleMenuMapper.insert(rm);
    }

    public Long createUser(String username, String rawPassword) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setStatus(1);
        userMapper.insert(user);
        return user.getId();
    }

    public void bindUserRole(Long userId, Long roleId) {
        SysUserRole ur = new SysUserRole();
        ur.setUserId(userId);
        ur.setRoleId(roleId);
        userRoleMapper.insert(ur);
    }

    // ---- 内部 ----

    /** 套餐圈定的全部菜单 id。 */
    private List<Long> menuIdsOfPackage(Long packageId) {
        if (packageId == null) {
            return List.of();
        }
        return packageMenuMapper.selectList(
                        Wrappers.<SysPackageMenu>lambdaQuery().eq(SysPackageMenu::getPackageId, packageId))
                .stream().map(SysPackageMenu::getMenuId).distinct().toList();
    }

    /**
     * 普通用户（USER 角色）默认不下放的「管理类页面」perm。
     *
     * <p>这些页面初始仅租户管理员可见（其权限动态等于套餐边界）；普通用户如需访问，
     * 由管理员在「角色↔菜单分配」中按需手动授予（如自建「文件管理员」角色）。
     */
    private static final List<String> USER_EXCLUDED_PERMS = List.of("system:file:list");

    /** 从菜单 id 集合中筛出 C 型页面菜单（供普通用户只读），排除管理类页面。 */
    private List<Long> pageMenuIds(List<Long> menuIds) {
        if (menuIds.isEmpty()) {
            return List.of();
        }
        return menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                        .in(SysMenu::getId, menuIds).eq(SysMenu::getType, "C")
                        .notIn(SysMenu::getPerm, USER_EXCLUDED_PERMS))
                .stream().map(SysMenu::getId).toList();
    }
}
