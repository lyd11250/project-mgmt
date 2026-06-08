package com.github.lyd11250.project.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建租户入参（超级管理员）：建租户并自动创建其租户管理员账号。
 */
@Data
public class TenantCreateDTO {

    @NotBlank(message = "租户名称不能为空")
    private String name;

    @NotBlank(message = "租户编码不能为空")
    private String code;

    @NotBlank(message = "管理员用户名不能为空")
    private String adminUsername;

    @NotBlank(message = "管理员密码不能为空")
    private String adminPassword;

    private String contact;
}
