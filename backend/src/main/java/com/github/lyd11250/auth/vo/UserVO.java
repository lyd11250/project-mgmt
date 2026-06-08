package com.github.lyd11250.auth.vo;

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

    private Integer status;

    private Long personId;

    private List<RoleVO> roles;

    private LocalDateTime createdAt;
}
