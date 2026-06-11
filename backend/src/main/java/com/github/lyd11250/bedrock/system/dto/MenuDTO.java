package com.github.lyd11250.bedrock.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 菜单创建/编辑入参（平台超管）。
 */
@Data
public class MenuDTO {

    /** 父菜单 id，根为 0。 */
    private Long parentId;

    /** M 目录 / C 菜单 / F 按钮。 */
    @NotBlank(message = "菜单类型不能为空")
    private String type;

    @NotBlank(message = "菜单名称不能为空")
    private String name;

    private String path;

    private String component;

    private String icon;

    /** 权限码 模块:资源:动作。 */
    private String perm;

    private Integer sort;

    private Integer visible;

    private Integer status;

    /** 页面是否启用前端缓存(keep-alive)：1 是，0 否。 */
    private Integer keepAlive;
}
