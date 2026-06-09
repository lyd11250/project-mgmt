package com.github.lyd11250.bedrock.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户视图对象（超级管理员视角）。
 */
@Data
public class TenantVO {

    private Long id;

    private String name;

    private String code;

    private Integer status;

    private Long packageId;

    private String packageName;

    private String contact;

    /** 订阅到期时间；null 表示永久。 */
    private LocalDateTime expireAt;

    private LocalDateTime createdAt;
}
