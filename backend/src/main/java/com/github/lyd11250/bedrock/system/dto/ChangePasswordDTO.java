package com.github.lyd11250.bedrock.system.dto;

import com.github.lyd11250.bedrock.common.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改自身密码入参（自助改密，需校验原密码）。
 */
@Data
public class ChangePasswordDTO {

    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @StrongPassword
    private String newPassword;
}
