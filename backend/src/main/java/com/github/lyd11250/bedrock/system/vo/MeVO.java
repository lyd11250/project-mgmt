package com.github.lyd11250.bedrock.system.vo;

import lombok.Data;

import java.util.List;

/**
 * 当前登录用户信息。
 */
@Data
public class MeVO {

    private Long userId;

    private String username;

    private String nickname;

    private Long tenantId;

    /** 当前头像文件 id（空=未设置）。 */
    private Long avatarFileId;

    private List<String> roles;

    private List<String> permissions;
}
