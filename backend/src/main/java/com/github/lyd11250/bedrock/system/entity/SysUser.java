package com.github.lyd11250.bedrock.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.lyd11250.bedrock.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统用户（可绑定 person，person 不必有登录账号）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private String username;

    private String passwordHash;

    /** 状态：1 启用，0 停用。 */
    private Integer status;

    /** 可选绑定的人员 ID。 */
    private Long personId;
}
