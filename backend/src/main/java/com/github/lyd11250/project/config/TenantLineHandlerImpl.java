package com.github.lyd11250.project.config;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 多租户处理器：为带 {@code tenant_id} 的表自动追加租户过滤条件。
 *
 * <p>租户 ID 取自 Sa-token 当前登录会话（登录时写入 {@code tenantId}）。
 * 未登录时返回系统租户 0（仅用于无业务查询的开放接口场景）。
 */
@Slf4j
@Component
public class TenantLineHandlerImpl implements TenantLineHandler {

    /** 会话中存放租户 ID 的键。 */
    public static final String SESSION_TENANT_ID = "tenantId";

    /** 不参与租户隔离的表（租户表本身、Flyway 历史表）。 */
    private static final Set<String> IGNORE_TABLES = Set.of(
            "tenant",
            "flyway_schema_history"
    );

    @Override
    public Expression getTenantId() {
        Long tenantId = 0L;
        try {
            Long override = TenantContext.get();
            if (override != null) {
                // 1) 上下文覆盖值（登录前定位/超管跨租户/建租户播种）
                tenantId = override;
            } else if (StpUtil.isLogin()) {
                // 2) 当前登录会话写入的租户
                Object value = StpUtil.getSession().get(SESSION_TENANT_ID);
                if (value != null) {
                    tenantId = Long.valueOf(value.toString());
                }
            }
        } catch (Exception e) {
            log.debug("获取当前租户失败，回退为系统租户 0：{}", e.getMessage());
        }
        return new LongValue(tenantId);
    }

    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }

    @Override
    public boolean ignoreTable(String tableName) {
        return IGNORE_TABLES.contains(tableName.toLowerCase());
    }
}
