package com.github.lyd11250.bedrock.config;

import cn.dev33.satoken.stp.StpUtil;
import com.github.lyd11250.bedrock.common.BusinessException;
import com.github.lyd11250.bedrock.common.ResultCode;
import com.github.lyd11250.bedrock.system.entity.Tenant;
import com.github.lyd11250.bedrock.system.mapper.TenantMapper;
import com.github.lyd11250.bedrock.system.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 请求级租户状态校验：在登录校验通过后，对已登录会话的租户做「停用 / 订阅过期」拦截。
 *
 * <p>惰性判定，<b>只读不写</b>——不修改 {@code tenant.status}。过期与否每次现算（与
 * {@link TenantService#isExpired} 共用同一逻辑、与 {@code AuthService.login} 行为一致），
 * 续费后即自动恢复访问，无需定时任务、不引入状态漂移。平台租户豁免。
 */
@Component
@RequiredArgsConstructor
public class TenantStatusInterceptor implements HandlerInterceptor {

    private final TenantMapper tenantMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 仅对已登录请求生效；未登录已由 SaInterceptor 处理
        if (!StpUtil.isLogin()) {
            return true;
        }
        Object tid = StpUtil.getSession().get(TenantLineHandlerImpl.SESSION_TENANT_ID);
        if (tid == null) {
            return true;
        }
        Tenant tenant = tenantMapper.selectById(Long.valueOf(tid.toString()));
        if (tenant == null) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "租户不存在或已被删除");
        }
        if (tenant.getStatus() != null && tenant.getStatus() == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "租户已停用，请联系平台");
        }
        if (TenantService.isExpired(tenant)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "租户订阅已过期，请联系平台续费");
        }
        return true;
    }
}
