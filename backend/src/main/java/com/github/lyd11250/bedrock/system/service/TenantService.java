package com.github.lyd11250.bedrock.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.lyd11250.bedrock.system.dto.TenantCreateDTO;
import com.github.lyd11250.bedrock.system.dto.TenantRenewDTO;
import com.github.lyd11250.bedrock.system.entity.SysPackage;
import com.github.lyd11250.bedrock.system.entity.Tenant;
import com.github.lyd11250.bedrock.system.mapper.SysPackageMapper;
import com.github.lyd11250.bedrock.system.mapper.TenantMapper;
import com.github.lyd11250.bedrock.system.vo.TenantVO;
import com.github.lyd11250.bedrock.common.BusinessException;
import com.github.lyd11250.bedrock.common.ResultCode;
import com.github.lyd11250.bedrock.system.RbacConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 租户管理（超级管理员）。{@code tenant} 表本身不参与多租户过滤，可跨租户查看/创建。
 */
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantMapper tenantMapper;
    private final SysPackageMapper packageMapper;
    private final SeedService seedService;

    public IPage<TenantVO> page(long current, long size) {
        Page<Tenant> page = tenantMapper.selectPage(Page.of(current, size),
                Wrappers.<Tenant>lambdaQuery().orderByDesc(Tenant::getCreatedAt));
        Map<Long, String> packageNames = packageMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysPackage::getId, SysPackage::getName));
        return page.convert(t -> toVO(t, packageNames::get));
    }

    /**
     * 创建租户：建租户行（绑套餐）+ 播种该租户的角色/租户管理员。
     */
    @Transactional
    public Long create(TenantCreateDTO dto) {
        Tenant exists = tenantMapper.selectOne(
                Wrappers.<Tenant>lambdaQuery().eq(Tenant::getCode, dto.getCode()));
        if (exists != null) {
            throw new BusinessException("租户编码已存在");
        }
        if (packageMapper.selectById(dto.getPackageId()) == null) {
            throw new BusinessException("套餐不存在");
        }
        Tenant tenant = new Tenant();
        tenant.setName(dto.getName());
        tenant.setCode(dto.getCode());
        tenant.setStatus(1);
        tenant.setPackageId(dto.getPackageId());
        tenant.setContact(dto.getContact());
        tenant.setExpireAt(dto.getExpireAt());
        tenantMapper.insert(tenant);

        seedService.seedTenant(tenant.getId(), dto.getPackageId(), dto.getAdminUsername(), dto.getAdminPassword());
        return tenant.getId();
    }

    /**
     * 续费/调整租户到期时间，可同时变更套餐。{@code expireAt} 为 null 表示设为永久；
     * {@code changePackageId} 为 null 表示套餐不变。套餐变更后鉴权边界实时生效（无需刷会话）。
     */
    @Transactional
    public void renew(Long id, TenantRenewDTO dto) {
        Tenant tenant = tenantMapper.selectById(id);
        if (tenant == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "租户不存在");
        }
        // 用 update wrapper 显式 set，确保 null（设为永久）也能落库（updateById 会跳过 null 字段）
        var update = Wrappers.<Tenant>lambdaUpdate()
                .eq(Tenant::getId, id)
                .set(Tenant::getExpireAt, dto.getExpireAt());
        if (dto.getChangePackageId() != null) {
            if (packageMapper.selectById(dto.getChangePackageId()) == null) {
                throw new BusinessException("套餐不存在");
            }
            update.set(Tenant::getPackageId, dto.getChangePackageId());
        }
        tenantMapper.update(null, update);
    }

    /**
     * 租户订阅是否已过期（惰性判定，不落库）。平台租户与无到期时间（永久）均视为未过期。
     */
    public static boolean isExpired(Tenant tenant) {
        return tenant != null
                && !RbacConstants.PLATFORM_TENANT_ID.equals(tenant.getId())
                && tenant.getExpireAt() != null
                && tenant.getExpireAt().isBefore(LocalDateTime.now());
    }

    private TenantVO toVO(Tenant t, Function<Long, String> packageNameOf) {
        TenantVO vo = new TenantVO();
        vo.setId(t.getId());
        vo.setName(t.getName());
        vo.setCode(t.getCode());
        vo.setStatus(t.getStatus());
        vo.setPackageId(t.getPackageId());
        vo.setPackageName(t.getPackageId() == null ? null : packageNameOf.apply(t.getPackageId()));
        vo.setContact(t.getContact());
        vo.setExpireAt(t.getExpireAt());
        vo.setExpired(isExpired(t));
        vo.setCreatedAt(t.getCreatedAt());
        return vo;
    }
}
