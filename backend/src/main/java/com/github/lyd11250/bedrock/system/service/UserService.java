package com.github.lyd11250.bedrock.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.lyd11250.bedrock.system.QuotaKeys;
import com.github.lyd11250.bedrock.system.dto.UserCreateDTO;
import com.github.lyd11250.bedrock.system.dto.UserUpdateDTO;
import com.github.lyd11250.bedrock.system.entity.SysRole;
import com.github.lyd11250.bedrock.system.entity.SysUser;
import com.github.lyd11250.bedrock.system.entity.SysUserRole;
import com.github.lyd11250.bedrock.system.mapper.SysRoleMapper;
import com.github.lyd11250.bedrock.system.mapper.SysUserMapper;
import com.github.lyd11250.bedrock.system.mapper.SysUserRoleMapper;
import com.github.lyd11250.bedrock.system.vo.RoleVO;
import com.github.lyd11250.bedrock.system.vo.UserVO;
import com.github.lyd11250.bedrock.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * 用户管理（本租户内，租户隔离由多租户插件自动完成）。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final QuotaService quotaService;

    public IPage<UserVO> page(long current, long size, String username) {
        Page<SysUser> page = userMapper.selectPage(Page.of(current, size),
                Wrappers.<SysUser>lambdaQuery()
                        .like(StringUtils.hasText(username), SysUser::getUsername, username)
                        .orderByDesc(SysUser::getCreatedAt));
        return page.convert(this::toVO);
    }

    @Transactional
    public Long create(UserCreateDTO dto) {
        Long exists = userMapper.selectCount(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, dto.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException("用户名已存在");
        }
        long currentCount = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery());
        quotaService.checkAndAssert(QuotaKeys.MAX_USERS, currentCount + 1);
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setStatus(1);
        user.setPersonId(dto.getPersonId());
        userMapper.insert(user);
        replaceRoles(user.getId(), dto.getRoleIds());
        return user.getId();
    }

    @Transactional
    public void update(Long id, UserUpdateDTO dto) {
        SysUser user = requireUser(id);
        if (dto.getStatus() != null) {
            user.setStatus(dto.getStatus());
        }
        user.setPersonId(dto.getPersonId());
        userMapper.updateById(user);
    }

    @Transactional
    public void delete(Long id) {
        requireUser(id);
        userMapper.deleteById(id);
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, id));
    }

    @Transactional
    public void resetPassword(Long id, String rawPassword) {
        SysUser user = requireUser(id);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        userMapper.updateById(user);
    }

    @Transactional
    public void assignRoles(Long id, List<Long> roleIds) {
        requireUser(id);
        replaceRoles(id, roleIds);
    }

    // ---- 内部 ----

    private void replaceRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId));
        if (roleIds == null) {
            return;
        }
        for (Long roleId : roleIds.stream().distinct().toList()) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
    }

    private SysUser requireUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private UserVO toVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setStatus(user.getStatus());
        vo.setPersonId(user.getPersonId());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setRoles(rolesOf(user.getId()));
        return vo;
    }

    private List<RoleVO> rolesOf(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(
                        Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).distinct().toList();
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roleMapper.selectList(Wrappers.<SysRole>lambdaQuery().in(SysRole::getId, roleIds))
                .stream().map(r -> {
                    RoleVO v = new RoleVO();
                    v.setId(r.getId());
                    v.setCode(r.getCode());
                    v.setName(r.getName());
                    return v;
                }).toList();
    }
}
