package com.github.lyd11250.auth.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.lyd11250.auth.RbacConstants;
import com.github.lyd11250.auth.dto.TenantCreateDTO;
import com.github.lyd11250.auth.service.TenantService;
import com.github.lyd11250.auth.vo.TenantVO;
import com.github.lyd11250.common.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租户管理接口（仅超级管理员）。
 */
@RestController
@RequestMapping("/api/v1/tenants")
@SaCheckRole(RbacConstants.ROLE_SUPER_ADMIN)
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @GetMapping
    public Result<IPage<TenantVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return Result.ok(tenantService.page(page, size));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody TenantCreateDTO dto) {
        return Result.ok(tenantService.create(dto));
    }
}
