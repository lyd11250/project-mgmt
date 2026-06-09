package com.github.lyd11250.bedrock.system.dto;

import lombok.Data;

import java.util.List;

/**
 * 分配菜单入参（套餐↔菜单 / 角色↔菜单，全量覆盖）。
 */
@Data
public class AssignMenusDTO {

    /** 菜单 ID 列表（全量覆盖）。 */
    private List<Long> menuIds;
}
