package com.github.lyd11250.project.auth.vo;

import lombok.Data;

import java.util.List;

/**
 * 当前登录用户信息。
 */
@Data
public class MeVO {

    private Long userId;

    private String username;

    private Long tenantId;

    private List<String> roles;

    private List<String> permissions;
}
