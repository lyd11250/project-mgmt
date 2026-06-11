package com.github.lyd11250.bedrock.system.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户续费/调整到期时间入参（超级管理员）。
 */
@Data
public class TenantRenewDTO {

    /** 新的订阅到期时间；null 表示设为永久。 */
    private LocalDateTime expireAt;

    /** 续费时变更套餐；null 表示套餐不变。 */
    private Long changePackageId;
}
