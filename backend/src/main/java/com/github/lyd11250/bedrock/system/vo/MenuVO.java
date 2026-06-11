package com.github.lyd11250.bedrock.system.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单视图对象（树形）。
 */
@Data
public class MenuVO {

    private Long id;

    private Long parentId;

    /** M 目录 / C 菜单 / F 按钮。 */
    private String type;

    private String name;

    private String path;

    private String component;

    private String icon;

    private String perm;

    private Integer sort;

    private Integer visible;

    private Integer status;

    /** 页面是否启用前端缓存(keep-alive)：1 是，0 否。 */
    private Integer keepAlive;

    private List<MenuVO> children = new ArrayList<>();
}
