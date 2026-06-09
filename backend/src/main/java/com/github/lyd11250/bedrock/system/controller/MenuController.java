package com.github.lyd11250.bedrock.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.github.lyd11250.bedrock.common.Result;
import com.github.lyd11250.bedrock.system.RbacConstants;
import com.github.lyd11250.bedrock.system.dto.MenuDTO;
import com.github.lyd11250.bedrock.system.service.MenuService;
import com.github.lyd11250.bedrock.system.vo.MenuVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 菜单接口：菜单目录管理（平台超管）+ 导航/可分配菜单下发。
 */
@RestController
@RequestMapping("/api/v1/system/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    /** 当前登录用户的导航菜单树（动态导航/路由用）。 */
    @GetMapping("/nav")
    public Result<List<MenuVO>> nav() {
        return Result.ok(menuService.nav());
    }

    /** 角色分配菜单时的可选菜单树（限当前租户套餐边界）。 */
    @GetMapping("/assignable")
    @SaCheckPermission("system:role:assignMenu")
    public Result<List<MenuVO>> assignable() {
        return Result.ok(menuService.assignableTree());
    }

    // 菜单为全局表（影响所有租户），其增删改查仅平台超管可用——
    // 用 @SaCheckRole 结构性兜底，防止 perm 经套餐泄漏给租户后越权改全局数据。
    @GetMapping
    @SaCheckRole(RbacConstants.ROLE_SUPER_ADMIN)
    @SaCheckPermission("system:menu:list")
    public Result<List<MenuVO>> tree() {
        return Result.ok(menuService.tree());
    }

    @PostMapping
    @SaCheckRole(RbacConstants.ROLE_SUPER_ADMIN)
    @SaCheckPermission("system:menu:create")
    public Result<Long> create(@Valid @RequestBody MenuDTO dto) {
        return Result.ok(menuService.create(dto));
    }

    @PutMapping("/{id}")
    @SaCheckRole(RbacConstants.ROLE_SUPER_ADMIN)
    @SaCheckPermission("system:menu:update")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody MenuDTO dto) {
        menuService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @SaCheckRole(RbacConstants.ROLE_SUPER_ADMIN)
    @SaCheckPermission("system:menu:delete")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return Result.ok();
    }
}
