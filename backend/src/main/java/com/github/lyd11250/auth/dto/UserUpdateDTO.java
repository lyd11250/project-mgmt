package com.github.lyd11250.auth.dto;

import lombok.Data;

/**
 * 更新用户入参（状态 / 绑定人员）。
 */
@Data
public class UserUpdateDTO {

    /** 状态：1 启用，0 停用。 */
    private Integer status;

    /** 可选绑定的人员 ID。 */
    private Long personId;
}
