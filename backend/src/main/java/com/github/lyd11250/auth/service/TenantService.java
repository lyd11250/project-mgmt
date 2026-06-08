package com.github.lyd11250.auth.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.lyd11250.auth.dto.TenantCreateDTO;
import com.github.lyd11250.auth.entity.Tenant;
import com.github.lyd11250.auth.mapper.TenantMapper;
import com.github.lyd11250.auth.vo.TenantVO;
import com.github.lyd11250.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 租户管理（超级管理员）。{@code tenant} 表本身不参与多租户过滤，可跨租户查看/创建。
 */
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantMapper tenantMapper;
    private final SeedService seedService;

    public IPage<TenantVO> page(long current, long size) {
        Page<Tenant> page = tenantMapper.selectPage(Page.of(current, size),
                Wrappers.<Tenant>lambdaQuery().orderByDesc(Tenant::getCreatedAt));
        return page.convert(this::toVO);
    }

    /**
     * 创建租户：建租户行 + 播种该租户的角色/权限/租户管理员。
     */
    @Transactional
    public Long create(TenantCreateDTO dto) {
        Tenant exists = tenantMapper.selectOne(
                Wrappers.<Tenant>lambdaQuery().eq(Tenant::getCode, dto.getCode()));
        if (exists != null) {
            throw new BusinessException("租户编码已存在");
        }
        Tenant tenant = new Tenant();
        tenant.setName(dto.getName());
        tenant.setCode(dto.getCode());
        tenant.setStatus(1);
        tenant.setContact(dto.getContact());
        tenantMapper.insert(tenant);

        seedService.seedTenant(tenant.getId(), dto.getAdminUsername(), dto.getAdminPassword());
        return tenant.getId();
    }

    private TenantVO toVO(Tenant t) {
        TenantVO vo = new TenantVO();
        vo.setId(t.getId());
        vo.setName(t.getName());
        vo.setCode(t.getCode());
        vo.setStatus(t.getStatus());
        vo.setContact(t.getContact());
        vo.setCreatedAt(t.getCreatedAt());
        return vo;
    }
}
