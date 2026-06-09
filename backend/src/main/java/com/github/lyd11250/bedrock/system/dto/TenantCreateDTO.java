package com.github.lyd11250.bedrock.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "套餐不能为空")
    private Long packageId;

    @NotBlank(message = "管理员用户名不能为空")
    private String adminUsername;

    @NotBlank(message = "管理员密码不能为空")
    private String adminPassword;

    private String contact;
}
