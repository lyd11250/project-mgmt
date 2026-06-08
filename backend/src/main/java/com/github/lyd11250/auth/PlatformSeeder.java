package com.github.lyd11250.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.lyd11250.auth.entity.Tenant;
import com.github.lyd11250.auth.mapper.TenantMapper;
import com.github.lyd11250.auth.service.SeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 平台种子初始化：首次启动时创建平台租户与超级管理员账号（幂等，已存在则跳过）。
 *
 * <p>默认账号见配置项 {@code app.super-admin.*}，默认密码须在首次登录后修改。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformSeeder implements ApplicationRunner {

    private final TenantMapper tenantMapper;
    private final SeedService seedService;

    @Value("${app.superadmin.username:admin}")
    private String superAdminUsername;

    @Value("${app.superadmin.password:admin123456}")
    private String superAdminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // tenant 表不参与多租户过滤，可直接按编码查
        Tenant platform = tenantMapper.selectOne(
                Wrappers.<Tenant>lambdaQuery().eq(Tenant::getCode, RbacConstants.PLATFORM_TENANT_CODE));
        if (platform != null) {
            return;
        }
        Tenant tenant = new Tenant();
        tenant.setId(RbacConstants.PLATFORM_TENANT_ID);
        tenant.setName("平台");
        tenant.setCode(RbacConstants.PLATFORM_TENANT_CODE);
        tenant.setStatus(1);
        tenantMapper.insert(tenant);

        seedService.seedPlatform(RbacConstants.PLATFORM_TENANT_ID, superAdminUsername, superAdminPassword);
        log.warn("已初始化平台租户与超级管理员账号：租户编码={}, 用户名={}，请尽快修改默认密码！",
                RbacConstants.PLATFORM_TENANT_CODE, superAdminUsername);
    }
}
