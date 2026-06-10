package com.github.lyd11250.bedrock.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.github.lyd11250.bedrock.common.Result;
import com.github.lyd11250.bedrock.system.dto.AssignMenusDTO;
import com.github.lyd11250.bedrock.system.dto.RoleDTO;
import com.github.lyd11250.bedrock.system.service.RoleService;
import com.github.lyd11250.bedrock.system.vo.RoleVO;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * 角色管理接口（本租户，租户管理员维护）。
 */
@Tag(name = "角色管理")
@RestController
@RequestMapping("/api/v1/system/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @SaCheckPermission("system:role:list")
    public Result<List<RoleVO>> list() {
        return Result.ok(roleService.list());
    }

    @PostMapping
    @SaCheckPermission("system:role:create")
    public Result<Long> create(@Valid @RequestBody RoleDTO dto) {
        return Result.ok(roleService.create(dto));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("system:role:update")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody RoleDTO dto) {
        roleService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("system:role:delete")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok();
    }

    @GetMapping("/{id}/menus")
    @SaCheckPermission("system:role:list")
    public Result<List<Long>> menus(@PathVariable Long id) {
        return Result.ok(roleService.menuIds(id));
    }

    @PutMapping("/{id}/menus")
    @SaCheckPermission("system:role:assignMenu")
    public Result<Void> assignMenus(@PathVariable Long id, @RequestBody AssignMenusDTO dto) {
        roleService.assignMenus(id, dto.getMenuIds());
        return Result.ok();
    }
}
