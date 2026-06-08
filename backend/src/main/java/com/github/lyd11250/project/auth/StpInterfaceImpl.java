package com.github.lyd11250.project.auth;

import cn.dev33.satoken.stp.StpInterface;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.lyd11250.project.auth.entity.SysPermission;
import com.github.lyd11250.project.auth.entity.SysRole;
import com.github.lyd11250.project.auth.entity.SysRolePermission;
import com.github.lyd11250.project.auth.entity.SysUserRole;
import com.github.lyd11250.project.auth.mapper.SysPermissionMapper;
import com.github.lyd11250.project.auth.mapper.SysRoleMapper;
import com.github.lyd11250.project.auth.mapper.SysRolePermissionMapper;
import com.github.lyd11250.project.auth.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Sa-token 鉴权数据源：按当前登录用户提供角色码与权限码。
 *
 * <p>查询时会话已写入 {@code tenantId}，多租户插件自动按租户过滤。
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysPermissionMapper permissionMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        List<Long> roleIds = roleIdsOf(loginId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> permIds = rolePermissionMapper.selectList(
                        Wrappers.<SysRolePermission>lambdaQuery().in(SysRolePermission::getRoleId, roleIds))
                .stream().map(SysRolePermission::getPermissionId).distinct().toList();
        if (permIds.isEmpty()) {
            return Collections.emptyList();
        }
        return permissionMapper.selectList(
                        Wrappers.<SysPermission>lambdaQuery().in(SysPermission::getId, permIds))
                .stream().map(SysPermission::getCode).distinct().toList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        List<Long> roleIds = roleIdsOf(loginId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roleMapper.selectList(
                        Wrappers.<SysRole>lambdaQuery().in(SysRole::getId, roleIds))
                .stream().map(SysRole::getCode).distinct().toList();
    }

    private List<Long> roleIdsOf(Object loginId) {
        Long userId = Long.valueOf(loginId.toString());
        return userRoleMapper.selectList(
                        Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).distinct().toList();
    }
}
