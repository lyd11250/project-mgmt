package com.github.lyd11250.bedrock.system.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户视图对象。
 */
@Data
public class UserVO {

    private Long id;

    private String username;

    private String nickname;

    private String phone;

    private Integer status;

    /** 当前头像文件 id（空=未设置）；前端据此拉取头像。 */
    private Long avatarFileId;

    private List<RoleVO> roles;

    private LocalDateTime createdAt;
}
