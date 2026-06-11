package com.github.lyd11250.bedrock.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.lyd11250.bedrock.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统用户。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private String username;

    private String passwordHash;

    /** 状态：1 启用，0 停用。 */
    private Integer status;

    /** 昵称（展示名）。 */
    private String nickname;

    /** 手机号。 */
    private String phone;

    /** 当前头像文件 id（引用 sys_file；空=未设置）。 */
    private Long avatarFileId;
}
