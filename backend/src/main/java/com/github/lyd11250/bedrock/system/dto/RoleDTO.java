package com.github.lyd11250.bedrock.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 角色创建/编辑入参（租户管理员）。
 */
@Data
public class RoleDTO {

    @NotBlank(message = "角色码不能为空")
    private String code;

    @NotBlank(message = "角色名称不能为空")
    private String name;
}
