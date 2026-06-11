package com.github.lyd11250.bedrock.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 创建用户入参（管理员后台建用户）。
 */
@Data
public class UserCreateDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 分配的角色 ID 列表。 */
    private List<Long> roleIds;
}
