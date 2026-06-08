package com.github.lyd11250.project.auth.dto;

import lombok.Data;

import java.util.List;

/**
 * 分配角色入参。
 */
@Data
public class AssignRolesDTO {

    /** 角色 ID 列表（全量覆盖）。 */
    private List<Long> roleIds;
}
