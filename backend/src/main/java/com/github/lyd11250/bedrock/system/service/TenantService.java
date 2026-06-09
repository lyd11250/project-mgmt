package com.github.lyd11250.bedrock.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.lyd11250.bedrock.system.dto.TenantCreateDTO;
import com.github.lyd11250.bedrock.system.entity.SysPackage;
import com.github.lyd11250.bedrock.system.entity.Tenant;
import com.github.lyd11250.bedrock.system.mapper.SysPackageMapper;
import com.github.lyd11250.bedrock.system.mapper.TenantMapper;
import com.github.lyd11250.bedrock.system.vo.TenantVO;
import com.github.lyd11250.bedrock.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        tenantMapper.insert(tenant);

        seedService.seedTenant(tenant.getId(), dto.getPackageId(), dto.getAdminUsername(), dto.getAdminPassword());
        return tenant.getId();
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
        vo.setCreatedAt(t.getCreatedAt());
        return vo;
    }
}
