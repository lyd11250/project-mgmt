package com.github.lyd11250.bedrock.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.github.lyd11250.bedrock.common.Result;
import com.github.lyd11250.bedrock.system.dto.AssignMenusDTO;
import com.github.lyd11250.bedrock.system.dto.PackageDTO;
import com.github.lyd11250.bedrock.system.service.PackageService;
import com.github.lyd11250.bedrock.system.vo.PackageVO;
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
 * 套餐管理接口（平台超管）。
 */
@RestController
@RequestMapping("/api/v1/system/packages")
@RequiredArgsConstructor
public class PackageController {

    private final PackageService packageService;

    @GetMapping
    @SaCheckPermission("system:package:list")
    public Result<List<PackageVO>> list() {
        return Result.ok(packageService.list());
    }

    @GetMapping("/{id}/menus")
    @SaCheckPermission("system:package:list")
    public Result<List<Long>> menus(@PathVariable Long id) {
        return Result.ok(packageService.menuIds(id));
    }

    @PostMapping
    @SaCheckPermission("system:package:create")
    public Result<Long> create(@Valid @RequestBody PackageDTO dto) {
        return Result.ok(packageService.create(dto));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("system:package:update")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody PackageDTO dto) {
        packageService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("system:package:delete")
    public Result<Void> delete(@PathVariable Long id) {
        packageService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/menus")
    @SaCheckPermission("system:package:assignMenu")
    public Result<Void> assignMenus(@PathVariable Long id, @RequestBody AssignMenusDTO dto) {
        packageService.assignMenus(id, dto.getMenuIds());
        return Result.ok();
    }
}
