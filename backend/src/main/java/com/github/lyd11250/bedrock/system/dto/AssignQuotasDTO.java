package com.github.lyd11250.bedrock.system.dto;

import lombok.Data;

import java.util.List;

/**
 * 套餐配额配置入参（全量覆盖某套餐的配额值）。
 */
@Data
public class AssignQuotasDTO {

    private List<QuotaValue> quotas;

    /**
     * 单项配额值：{@code quotaValue} 为 {@code null} 或 {@code -1} 视为不限（不落库）。
     */
    @Data
    public static class QuotaValue {

        /** 配额定义 id（sys_quota_def.id）。 */
        private Long quotaId;

        /** 配额上限，-1/空 表示不限。 */
        private Long quotaValue;
    }
}
