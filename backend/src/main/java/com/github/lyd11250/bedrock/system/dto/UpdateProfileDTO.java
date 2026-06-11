package com.github.lyd11250.bedrock.system.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新自身资料入参（个人中心）。
 * 仅允许改昵称、手机号；用户名、状态、密码不在此处变更。
 */
@Data
public class UpdateProfileDTO {

    @Size(max = 64, message = "昵称长度不能超过 64")
    private String nickname;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
