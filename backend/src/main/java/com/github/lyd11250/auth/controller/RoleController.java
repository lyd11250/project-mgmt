package com.github.lyd11250.auth.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.github.lyd11250.auth.service.RoleService;
import com.github.lyd11250.auth.vo.RoleVO;
import com.github.lyd11250.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色查询接口（供分配角色使用）。
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @SaCheckPermission("role:list")
    public Result<List<RoleVO>> list() {
        return Result.ok(roleService.list());
    }
}
