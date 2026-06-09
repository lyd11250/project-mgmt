package com.github.lyd11250.bedrock.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.lyd11250.bedrock.common.BusinessException;
import com.github.lyd11250.bedrock.system.entity.SysPackage;
import com.github.lyd11250.bedrock.system.entity.SysPackageMenu;
import com.github.lyd11250.bedrock.system.entity.SysPackageQuota;
import com.github.lyd11250.bedrock.system.entity.SysQuotaDef;
import com.github.lyd11250.bedrock.system.dto.AssignQuotasDTO;
import com.github.lyd11250.bedrock.system.dto.PackageDTO;
import com.github.lyd11250.bedrock.system.mapper.SysPackageMapper;
import com.github.lyd11250.bedrock.system.mapper.SysPackageMenuMapper;
import com.github.lyd11250.bedrock.system.mapper.SysPackageQuotaMapper;
import com.github.lyd11250.bedrock.system.mapper.SysQuotaDefMapper;
import com.github.lyd11250.bedrock.system.vo.PackageQuotaVO;
import com.github.lyd11250.bedrock.system.vo.PackageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 套餐管理（平台超管）：套餐 CRUD、套餐↔菜单分配、套餐配额配置。全局表，不参与租户隔离。
 */
@Service
@RequiredArgsConstructor
public class PackageService {

    private final SysPackageMapper packageMapper;
    private final SysPackageMenuMapper packageMenuMapper;
    private final SysPackageQuotaMapper packageQuotaMapper;
    private final SysQuotaDefMapper quotaDefMapper;

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

    /**
     * 列出某套餐的配额：以配额定义字典为全集，左连接该套餐的已配置值（未配置=不限 -1）。
     */
    public List<PackageQuotaVO> listQuotas(Long packageId) {
        require(packageId);
        Map<Long, Long> values = packageQuotaMapper.selectList(Wrappers.<SysPackageQuota>lambdaQuery()
                        .eq(SysPackageQuota::getPackageId, packageId))
                .stream().collect(Collectors.toMap(SysPackageQuota::getQuotaId, SysPackageQuota::getQuotaValue));
        return quotaDefMapper.selectList(Wrappers.<SysQuotaDef>lambdaQuery()
                        .orderByAsc(SysQuotaDef::getSort).orderByAsc(SysQuotaDef::getId))
                .stream().map(def -> {
                    PackageQuotaVO vo = new PackageQuotaVO();
                    vo.setQuotaId(def.getId());
                    vo.setQuotaKey(def.getQuotaKey());
                    vo.setQuotaName(def.getName());
                    vo.setRemark(def.getRemark());
                    vo.setQuotaValue(values.getOrDefault(def.getId(), -1L));
                    return vo;
                }).toList();
    }

    /** 全量覆盖某套餐配额：删后按入参重建（仅写入有限上限，-1/空=不限不落库）。 */
    @Transactional
    public void saveQuotas(Long packageId, List<AssignQuotasDTO.QuotaValue> quotas) {
        require(packageId);
        packageQuotaMapper.delete(Wrappers.<SysPackageQuota>lambdaQuery().eq(SysPackageQuota::getPackageId, packageId));
        if (quotas == null) {
            return;
        }
        Map<Long, SysQuotaDef> defs = quotaDefMapper.selectList(null)
                .stream().collect(Collectors.toMap(SysQuotaDef::getId, Function.identity()));
        for (AssignQuotasDTO.QuotaValue q : quotas) {
            if (q.getQuotaId() == null || !defs.containsKey(q.getQuotaId())) {
                continue;
            }
            Long value = q.getQuotaValue();
            if (value == null || value < 0) {
                continue;
            }
            SysPackageQuota entity = new SysPackageQuota();
            entity.setPackageId(packageId);
            entity.setQuotaId(q.getQuotaId());
            entity.setQuotaValue(value);
            packageQuotaMapper.insert(entity);
        }
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
        return vo;
    }
}
