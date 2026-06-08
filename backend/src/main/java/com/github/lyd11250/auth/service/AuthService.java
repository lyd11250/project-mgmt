package com.github.lyd11250.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.lyd11250.auth.dto.LoginDTO;
import com.github.lyd11250.auth.entity.SysUser;
import com.github.lyd11250.auth.entity.Tenant;
import com.github.lyd11250.auth.mapper.SysUserMapper;
import com.github.lyd11250.auth.mapper.TenantMapper;
import com.github.lyd11250.auth.vo.LoginVO;
import com.github.lyd11250.auth.vo.MeVO;
import com.github.lyd11250.common.BusinessException;
import com.github.lyd11250.common.ResultCode;
import com.github.lyd11250.config.TenantContext;
import com.github.lyd11250.config.TenantLineHandlerImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务：登录 / 登出 / 当前用户。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final TenantMapper tenantMapper;
    private final SysUserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * 登录：按租户编码定位租户 → 在该租户内校验用户名密码 → 发令牌并写入会话租户。
     */
    public LoginVO login(LoginDTO dto) {
        // tenant 表在多租户忽略名单中，按编码直接查
        Tenant tenant = tenantMapper.selectOne(
                Wrappers.<Tenant>lambdaQuery().eq(Tenant::getCode, dto.getTenantCode()));
        if (tenant == null || (tenant.getStatus() != null && tenant.getStatus() == 0)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "租户编码、用户名或密码错误");
        }

        // 在目标租户上下文中查用户
        SysUser user = TenantContext.runAs(tenant.getId(), () -> userMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, dto.getUsername())));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "租户编码、用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "账号已停用");
        }

        StpUtil.login(user.getId());
        StpUtil.getSession().set(TenantLineHandlerImpl.SESSION_TENANT_ID, tenant.getId());
        return new LoginVO(StpUtil.getTokenName(), StpUtil.getTokenValue());
    }

    public void logout() {
        StpUtil.logout();
    }

    /** 当前登录用户信息（角色/权限由 Sa-token 经 StpInterface 加载）。 */
    public MeVO me() {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userMapper.selectById(userId);
        MeVO vo = new MeVO();
        vo.setUserId(userId);
        vo.setUsername(user != null ? user.getUsername() : null);
        Object tenantId = StpUtil.getSession().get(TenantLineHandlerImpl.SESSION_TENANT_ID);
        vo.setTenantId(tenantId != null ? Long.valueOf(tenantId.toString()) : null);
        vo.setRoles(StpUtil.getRoleList());
        vo.setPermissions(StpUtil.getPermissionList());
        return vo;
    }
}
