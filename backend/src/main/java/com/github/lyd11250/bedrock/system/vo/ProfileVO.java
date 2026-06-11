package com.github.lyd11250.bedrock.system.vo;

import lombok.Data;

import java.util.List;

/**
 * 个人中心视图对象（当前登录用户自身资料）。
 */
@Data
public class ProfileVO {

    private Long id;

    /** 用户名（只读，不可自助修改）。 */
    private String username;

    private String nickname;

    private String phone;

    private Long tenantId;

    /** 角色名称列表（展示用）。 */
    private List<RoleVO> roles;
}
