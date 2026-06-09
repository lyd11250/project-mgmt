package com.github.lyd11250.bedrock.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.lyd11250.bedrock.common.BusinessException;
import com.github.lyd11250.bedrock.system.entity.SysPackage;
import com.github.lyd11250.bedrock.system.entity.SysPackageMenu;
import com.github.lyd11250.bedrock.system.entity.SysPackageQuota;
import com.github.lyd11250.bedrock.system.dto.PackageDTO;
import com.github.lyd11250.bedrock.system.mapper.SysPackageMapper;
import com.github.lyd11250.bedrock.system.mapper.SysPackageMenuMapper;
import com.github.lyd11250.bedrock.system.mapper.SysPackageQuotaMapper;
import com.github.lyd11250.bedrock.system.vo.PackageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 套餐管理（平台超管）：套餐 CRUD、套餐↔菜单分配、套餐配额（键值模型）。全局表，不参与租户隔离。
 */
@Service
@RequiredArgsConstructor
public class PackageService {

    private final SysPackageMapper packageMapper;
    private final SysPackageMenuMapper packageMenuMapper;
    private final SysPackageQuotaMapper packageQuotaMapper;

    public List<PackageVO> list() {
        return packageMapper.selectList(Wrappers.<SysPackage>lambdaQuery().orderByAsc(SysPackage::getId))
                .stream().map(this::toVO).toList();
    }

    @Transactional
    public Long create(PackageDTO dto) {
        if (packageMapper.selectCount(Wrappers.<SysPackage>lambdaQuery().eq(SysPackage::getCode, dto.getCode())) > 0) {
            throw new BusinessException("套餐编码已存在");
        }
        SysPackage p = new SysPackage();
        p.setName(dto.getName());
        p.setCode(dto.getCode());
        p.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        p.setRemark(dto.getRemark());
        packageMapper.insert(p);
        replaceQuotas(p.getId(), dto.getQuotas());
        return p.getId();
    }

    @Transactional
    public void update(Long id, PackageDTO dto) {
        SysPackage p = require(id);
        p.setName(dto.getName());
        p.setCode(dto.getCode());
        if (dto.getStatus() != null) {
            p.setStatus(dto.getStatus());
        }
        p.setRemark(dto.getRemark());
        packageMapper.updateById(p);
        if (dto.getQuotas() != null) {
            replaceQuotas(id, dto.getQuotas());
        }
    }

    @Transactional
    public void delete(Long id) {
        require(id);
        packageMapper.deleteById(id);
        packageMenuMapper.delete(Wrappers.<SysPackageMenu>lambdaQuery().eq(SysPackageMenu::getPackageId, id));
        packageQuotaMapper.delete(Wrappers.<SysPackageQuota>lambdaQuery().eq(SysPackageQuota::getPackageId, id));
    }

    public List<Long> menuIds(Long packageId) {
        require(packageId);
        return packageMenuMapper.selectList(
                        Wrappers.<SysPackageMenu>lambdaQuery().eq(SysPackageMenu::getPackageId, packageId))
                .stream().map(SysPackageMenu::getMenuId).distinct().toList();
    }

    @Transactional
    public void assignMenus(Long packageId, List<Long> menuIds) {
        require(packageId);
        packageMenuMapper.delete(Wrappers.<SysPackageMenu>lambdaQuery().eq(SysPackageMenu::getPackageId, packageId));
        if (menuIds == null) {
            return;
        }
        for (Long menuId : menuIds.stream().distinct().toList()) {
            SysPackageMenu pm = new SysPackageMenu();
            pm.setPackageId(packageId);
            pm.setMenuId(menuId);
            packageMenuMapper.insert(pm);
        }
    }

    /** 全量覆盖套餐配额：删后按 map 重建（仅写入非空 key）。 */
    private void replaceQuotas(Long packageId, Map<String, Long> quotas) {
        packageQuotaMapper.delete(Wrappers.<SysPackageQuota>lambdaQuery().eq(SysPackageQuota::getPackageId, packageId));
        if (quotas == null) {
            return;
        }
        quotas.forEach((key, value) -> {
            if (!StringUtils.hasText(key) || value == null) {
                return;
            }
            SysPackageQuota q = new SysPackageQuota();
            q.setPackageId(packageId);
            q.setQuotaKey(key.trim());
            q.setQuotaValue(value);
            packageQuotaMapper.insert(q);
        });
    }

    private Map<String, Long> quotasOf(Long packageId) {
        Map<String, Long> map = new LinkedHashMap<>();
        packageQuotaMapper.selectList(Wrappers.<SysPackageQuota>lambdaQuery()
                        .eq(SysPackageQuota::getPackageId, packageId))
                .forEach(q -> map.put(q.getQuotaKey(), q.getQuotaValue()));
        return map;
    }

    private SysPackage require(Long id) {
        SysPackage p = packageMapper.selectById(id);
        if (p == null) {
            throw new BusinessException("套餐不存在");
        }
        return p;
    }

    private PackageVO toVO(SysPackage p) {
        PackageVO vo = new PackageVO();
        vo.setId(p.getId());
        vo.setName(p.getName());
        vo.setCode(p.getCode());
        vo.setStatus(p.getStatus());
        vo.setRemark(p.getRemark());
        vo.setQuotas(quotasOf(p.getId()));
        return vo;
    }
}
