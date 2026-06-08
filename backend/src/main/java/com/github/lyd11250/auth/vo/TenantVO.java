package com.github.lyd11250.auth.vo;

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

    private String contact;

    private LocalDateTime createdAt;
}
