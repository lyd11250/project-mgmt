package com.github.lyd11250.bedrock.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.lyd11250.bedrock.common.BusinessException;
import com.github.lyd11250.bedrock.config.TenantLineHandlerImpl;
import com.github.lyd11250.bedrock.system.RbacConstants;
import com.github.lyd11250.bedrock.system.dto.RoleDTO;
import com.github.lyd11250.bedrock.system.entity.SysPackageMenu;
import com.github.lyd11250.bedrock.system.entity.SysRole;
import com.github.lyd11250.bedrock.system.entity.SysRoleMenu;
import com.github.lyd11250.bedrock.system.entity.SysUserRole;
import com.github.lyd11250.bedrock.system.entity.Tenant;
import com.github.lyd11250.bedrock.system.mapper.SysPackageMenuMapper;
import com.github.lyd11250.bedrock.system.mapper.SysRoleMapper;
import com.github.lyd11250.bedrock.system.mapper.SysRoleMenuMapper;
import com.github.lyd11250.bedrock.system.mapper.SysUserRoleMapper;
import com.github.lyd11250.bedrock.system.mapper.TenantMapper;
import com.github.lyd11250.bedrock.system.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 角色管理（本租户，租户管理员维护）：角色 CRUD 与角色↔菜单分配（限套餐边界内）。
 */
@Service
@RequiredArgsConstructor
public class RoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysPackageMenuMapper packageMenuMapper;
    private final TenantMapper tenantMapper;

    public List<RoleVO> list() {
        return roleMapper.selectList(Wrappers.<SysRole>lambdaQuery().orderByAsc(SysRole::getId))
                .stream().map(this::toVO).toList();
    }

    public Long create(RoleDTO dto) {
        if (RbacConstants.isReservedRoleCode(dto.getCode())) {
            throw new BusinessException("角色码不可用");
        }
        if (roleMapper.selectCount(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getCode, dto.getCode())) > 0) {
            throw new BusinessException("角色码已存在");
        }
        SysRole role = new SysRole();
        role.setCode(dto.getCode());
        role.setName(dto.getName());
        roleMapper.insert(role);
        return role.getId();
    }

    public void update(Long id, RoleDTO dto) {
        SysRole role = require(id);
        role.setName(dto.getName());
        roleMapper.updateById(role);
    }

    @Transactional
    public void delete(Long id) {
        require(id);
        roleMapper.deleteById(id);
        roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, id));
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getRoleId, id));
    }

    public List<Long> menuIds(Long roleId) {
        require(roleId);
        return roleMenuMapper.selectList(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, roleId))
                .stream().map(SysRoleMenu::getMenuId).distinct().toList();
    }

    /** 分配菜单（全量覆盖），仅接受套餐边界内的菜单。 */
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        require(roleId);
        Set<Long> boundary = boundaryMenuIds();
        roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, roleId));
        if (menuIds == null) {
            return;
        }
        for (Long menuId : menuIds.stream().distinct().toList()) {
            if (!boundary.contains(menuId)) {
                continue;   // 越界菜单忽略
            }
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            roleMenuMapper.insert(rm);
        }
    }

    // ---- 内部 ----

    /** 当前租户套餐圈定的菜单 id 集合。 */
    private Set<Long> boundaryMenuIds() {
        Object tid = StpUtil.getSession().get(TenantLineHandlerImpl.SESSION_TENANT_ID);
        if (tid == null) {
            return Set.of();
        }
        Tenant tenant = tenantMapper.selectById(Long.valueOf(tid.toString()));
        if (tenant == null || tenant.getPackageId() == null) {
            return Set.of();
        }
        return packageMenuMapper.selectList(
                        Wrappers.<SysPackageMenu>lambdaQuery().eq(SysPackageMenu::getPackageId, tenant.getPackageId()))
                .stream().map(SysPackageMenu::getMenuId).collect(java.util.stream.Collectors.toSet());
    }

    private SysRole require(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        return role;
    }

    private RoleVO toVO(SysRole r) {
        RoleVO v = new RoleVO();
        v.setId(r.getId());
        v.setCode(r.getCode());
        v.setName(r.getName());
        return v;
    }
}
