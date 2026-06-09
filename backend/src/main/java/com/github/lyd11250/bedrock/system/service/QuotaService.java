package com.github.lyd11250.bedrock.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.lyd11250.bedrock.common.BusinessException;
import com.github.lyd11250.bedrock.config.TenantLineHandlerImpl;
import com.github.lyd11250.bedrock.system.QuotaKeys;
import com.github.lyd11250.bedrock.system.entity.SysPackageQuota;
import com.github.lyd11250.bedrock.system.entity.SysQuotaDef;
import com.github.lyd11250.bedrock.system.entity.Tenant;
import com.github.lyd11250.bedrock.system.mapper.SysPackageQuotaMapper;
import com.github.lyd11250.bedrock.system.mapper.SysQuotaDefMapper;
import com.github.lyd11250.bedrock.system.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 套餐配额校验（通用，不绑定具体 quota_key）。
 *
 * <p>按当前会话租户 → {@code package_id} → {@code sys_package_quota} 查上限：
 * {@code -1} 或无记录视为不限（缺省放行）。biz 模块在自身写操作处调用
 * {@link #checkAndAssert(String, long)} 即可接入配额，无需改基座。
 */
@Service
@RequiredArgsConstructor
public class QuotaService {

    private final TenantMapper tenantMapper;
    private final SysPackageQuotaMapper quotaMapper;
    private final SysQuotaDefMapper quotaDefMapper;

    /**
     * 当前租户套餐对指定配额的上限；{@code -1} 表示不限（含未定义/未配置）。
     */
    public long limitOf(String quotaKey) {
        Object tid = StpUtil.getSession().get(TenantLineHandlerImpl.SESSION_TENANT_ID);
        if (tid == null) {
            return QuotaKeys.UNLIMITED;
        }
        Tenant tenant = tenantMapper.selectById(Long.valueOf(tid.toString()));
        if (tenant == null || tenant.getPackageId() == null) {
            return QuotaKeys.UNLIMITED;
        }
        SysQuotaDef def = quotaDefMapper.selectOne(Wrappers.<SysQuotaDef>lambdaQuery()
                .eq(SysQuotaDef::getQuotaKey, quotaKey));
        if (def == null) {
            return QuotaKeys.UNLIMITED;
        }
        SysPackageQuota quota = quotaMapper.selectOne(Wrappers.<SysPackageQuota>lambdaQuery()
                .eq(SysPackageQuota::getPackageId, tenant.getPackageId())
                .eq(SysPackageQuota::getQuotaId, def.getId()));
        return quota == null || quota.getQuotaValue() == null ? QuotaKeys.UNLIMITED : quota.getQuotaValue();
    }

    /**
     * 校验意向总量是否超出配额，超出则抛业务异常。
     *
     * @param quotaKey      配额标识
     * @param intendedTotal 操作完成后的预期总量（如建用户后的用户总数）
     */
    public void checkAndAssert(String quotaKey, long intendedTotal) {
        long limit = limitOf(quotaKey);
        if (limit != QuotaKeys.UNLIMITED && intendedTotal > limit) {
            throw new BusinessException("已达套餐配额上限（" + quotaKey + "，上限 " + limit + "）");
        }
    }
}
