package com.github.lyd11250.bedrock.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.lyd11250.bedrock.common.GlobalBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 全局菜单（菜单与按钮权限合并为一棵树）。
 *
 * <p>{@code type}：{@code M} 目录（仅导航分组）/ {@code C} 菜单（对应页面路由）/ {@code F} 按钮（挂 {@code perm}，无路由）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends GlobalBaseEntity {

    /** 父菜单 id，根为 0。 */
    private Long parentId;

    /** M 目录 / C 菜单 / F 按钮。 */
    private String type;

    private String name;

    /** 路由路径（C 型）。 */
    private String path;

    /** 前端组件标识（C 型，如 system/UserList）。 */
    private String component;

    private String icon;

    /** 权限码 模块:资源:动作（C/F 型）。 */
    private String perm;

    private Integer sort;

    /** 是否在导航显示：1 显示，0 隐藏。 */
    private Integer visible;

    /** 状态：1 启用，0 停用。 */
    private Integer status;
}
