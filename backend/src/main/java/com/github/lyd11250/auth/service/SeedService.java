package com.github.lyd11250.auth.service;

import com.github.lyd11250.auth.RbacConstants;
import com.github.lyd11250.auth.entity.SysPermission;
import com.github.lyd11250.auth.entity.SysRole;
import com.github.lyd11250.auth.entity.SysRolePermission;
import com.github.lyd11250.auth.entity.SysUser;
import com.github.lyd11250.auth.entity.SysUserRole;
import com.github.lyd11250.auth.mapper.SysPermissionMapper;
import com.github.lyd11250.auth.mapper.SysRoleMapper;
import com.github.lyd11250.auth.mapper.SysRolePermissionMapper;
import com.github.lyd11250.auth.mapper.SysUserMapper;
import com.github.lyd11250.auth.mapper.SysUserRoleMapper;
import com.github.lyd11250.config.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 种子/初始化逻辑：为「平台租户」与「新建租户」播种角色、权限、管理员。
 *
 * <p>所有写入均在 {@link TenantContext#runAs} 指定的租户上下文中执行，
 * 使多租户插件把数据落到目标租户。
 */
@Service
@RequiredArgsConstructor
public class SeedService {

    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * 为平台租户播种：SUPER_ADMIN 角色 + 通配权限 + 超管账号。
     */
    @Transactional
    public void seedPlatform(Long platformTenantId, String adminUsername, String adminRawPassword) {
        TenantContext.runAs(platformTenantId, () -> {
            SysPermission all = createPermission(RbacConstants.PERMISSION_ALL, "全部权限");
            Long superRoleId = createRole(RbacConstants.ROLE_SUPER_ADMIN, "超级管理员");
            bindRolePermission(superRoleId, all.getId());
            Long userId = createUser(adminUsername, adminRawPassword);
            bindUserRole(userId, superRoleId);
            return null;
        });
    }

    /**
     * 为新建租户播种：TENANT_ADMIN / USER 角色 + 权限目录 + 租户管理员账号。
     */
    @Transactional
    public void seedTenant(Long tenantId, String adminUsername, String adminRawPassword) {
        TenantContext.runAs(tenantId, () -> {
            // 权限目录
            Map<String, Long> permIds = new java.util.HashMap<>();
            for (Map.Entry<String, String> e : RbacConstants.TENANT_PERMISSIONS.entrySet()) {
                SysPermission p = createPermission(e.getKey(), e.getValue());
                permIds.put(e.getKey(), p.getId());
            }
            // 租户管理员：拥有全部权限
            Long adminRoleId = createRole(RbacConstants.ROLE_TENANT_ADMIN, "租户管理员");
            for (Long pid : permIds.values()) {
                bindRolePermission(adminRoleId, pid);
            }
            // 普通用户：仅只读
            Long userRoleId = createRole(RbacConstants.ROLE_USER, "普通用户");
            for (String code : RbacConstants.USER_PERMISSIONS) {
                bindRolePermission(userRoleId, permIds.get(code));
            }
            // 创建租户管理员账号
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

    public SysPermission createPermission(String code, String name) {
        SysPermission p = new SysPermission();
        p.setCode(code);
        p.setName(name);
        permissionMapper.insert(p);
        return p;
    }

    public void bindRolePermission(Long roleId, Long permissionId) {
        SysRolePermission rp = new SysRolePermission();
        rp.setRoleId(roleId);
        rp.setPermissionId(permissionId);
        rolePermissionMapper.insert(rp);
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
}
