package com.github.lyd11250.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录入参：租户编码 + 用户名 + 密码。
 */
@Data
public class LoginDTO {

    @NotBlank(message = "租户编码不能为空")
    private String tenantCode;

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
