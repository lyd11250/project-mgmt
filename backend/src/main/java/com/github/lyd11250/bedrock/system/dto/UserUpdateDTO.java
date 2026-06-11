package com.github.lyd11250.bedrock.system.dto;

import lombok.Data;

/**
 * 更新用户入参（状态）。
 */
@Data
public class UserUpdateDTO {

    /** 状态：1 启用，0 停用。 */
    private Integer status;
}
