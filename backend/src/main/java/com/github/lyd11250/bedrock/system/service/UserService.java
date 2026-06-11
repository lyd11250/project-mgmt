package com.github.lyd11250.bedrock.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.lyd11250.bedrock.config.TenantLineHandlerImpl;
import com.github.lyd11250.bedrock.system.QuotaKeys;
import com.github.lyd11250.bedrock.system.dto.ChangePasswordDTO;
import com.github.lyd11250.bedrock.system.dto.UpdateProfileDTO;
import com.github.lyd11250.bedrock.system.dto.UserCreateDTO;
import com.github.lyd11250.bedrock.system.dto.UserUpdateDTO;
import com.github.lyd11250.bedrock.system.entity.SysRole;
import com.github.lyd11250.bedrock.system.entity.SysUser;
import com.github.lyd11250.bedrock.system.entity.SysUserRole;
import com.github.lyd11250.bedrock.system.mapper.SysRoleMapper;
import com.github.lyd11250.bedrock.system.mapper.SysUserMapper;
import com.github.lyd11250.bedrock.system.mapper.SysUserRoleMapper;
import com.github.lyd11250.bedrock.system.vo.ProfileVO;
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
    private final PermissionCacheService permissionCache;
    private final FileService fileService;

    /** 允许的头像图片类型。 */
    private static final java.util.Set<String> AVATAR_CONTENT_TYPES =
            java.util.Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

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
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setStatus(1);
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
        userMapper.updateById(user);
        // 停用用户后主动踢出其全部在线会话，避免既有登录态在 timeout 内继续可用
        if (dto.getStatus() != null && dto.getStatus() == 0) {
            StpUtil.kickout(id);
        }
    }

    @Transactional
    public void delete(Long id) {
        requireUser(id);
        userMapper.deleteById(id);
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, id));
        permissionCache.evictUserInCurrentTenant(id);
        // 删除用户后踢出其全部在线会话
        StpUtil.kickout(id);
    }

    @Transactional
    public void resetPassword(Long id, String rawPassword) {
        SysUser user = requireUser(id);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        userMapper.updateById(user);
        // 管理员重置密码后强制该用户重新登录
        StpUtil.kickout(id);
    }

    @Transactional
    public void assignRoles(Long id, List<Long> roleIds) {
        requireUser(id);
        replaceRoles(id, roleIds);
    }

    // ---- 自助（当前登录用户）----

    /** 个人中心：查询当前登录用户资料。 */
    public ProfileVO getProfile() {
        SysUser user = requireUser(StpUtil.getLoginIdAsLong());
        ProfileVO vo = new ProfileVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        Object tenantId = StpUtil.getSession().get(TenantLineHandlerImpl.SESSION_TENANT_ID);
        vo.setTenantId(tenantId != null ? Long.valueOf(tenantId.toString()) : null);
        vo.setAvatarFileId(user.getAvatarFileId());
        vo.setRoles(rolesOf(user.getId()));
        return vo;
    }

    /** 个人中心：上传/更换当前登录用户头像，返回新头像文件 id。 */
    @Transactional
    public Long updateAvatar(org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("头像文件不能为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || !AVATAR_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException("头像仅支持 JPG/PNG/WEBP/GIF 格式");
        }
        SysUser user = requireUser(StpUtil.getLoginIdAsLong());
        Long oldAvatarId = user.getAvatarFileId();

        Long newAvatarId = fileService.storeFile(file, "system:user:avatar").getId();
        user.setAvatarFileId(newAvatarId);
        userMapper.updateById(user);

        // 软删旧头像记录（物理对象留待定时任务清理）
        if (oldAvatarId != null) {
            fileService.softDelete(oldAvatarId);
        }
        return newAvatarId;
    }

    /** 读取指定用户的头像文件流（本租户内可见，租户隔离由插件保证）。 */
    public FileService.DownloadFile getAvatar(Long userId) {
        SysUser user = requireUser(userId);
        if (user.getAvatarFileId() == null) {
            throw new BusinessException(com.github.lyd11250.bedrock.common.ResultCode.NOT_FOUND.getCode(), "用户未设置头像");
        }
        return fileService.loadInline(user.getAvatarFileId());
    }

    /** 个人中心：更新当前登录用户资料（仅昵称、手机号）。 */
    @Transactional
    public void updateProfile(UpdateProfileDTO dto) {
        SysUser user = requireUser(StpUtil.getLoginIdAsLong());
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        userMapper.updateById(user);
    }

    /** 自助改密：校验原密码后更新为新密码。 */
    @Transactional
    public void changeOwnPassword(ChangePasswordDTO dto) {
        SysUser user = requireUser(StpUtil.getLoginIdAsLong());
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException("原密码不正确");
        }
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
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
        permissionCache.evictUserInCurrentTenant(userId);
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
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        vo.setStatus(user.getStatus());
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
