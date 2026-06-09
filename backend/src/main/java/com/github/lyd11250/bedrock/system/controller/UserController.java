package com.github.lyd11250.bedrock.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.lyd11250.bedrock.system.dto.AssignRolesDTO;
import com.github.lyd11250.bedrock.system.dto.ResetPasswordDTO;
import com.github.lyd11250.bedrock.system.dto.UserCreateDTO;
import com.github.lyd11250.bedrock.system.dto.UserUpdateDTO;
import com.github.lyd11250.bedrock.system.service.UserService;
import com.github.lyd11250.bedrock.system.vo.UserVO;
import com.github.lyd11250.bedrock.common.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理接口（租户内，按权限校验）。
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @SaCheckPermission("system:user:list")
    public Result<IPage<UserVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String username) {
        return Result.ok(userService.page(page, size, username));
    }

    @PostMapping
    @SaCheckPermission("system:user:create")
    public Result<Long> create(@Valid @RequestBody UserCreateDTO dto) {
        return Result.ok(userService.create(dto));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("system:user:update")
    public Result<Void> update(@PathVariable Long id, @RequestBody UserUpdateDTO dto) {
        userService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("system:user:delete")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/password")
    @SaCheckPermission("system:user:resetPwd")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordDTO dto) {
        userService.resetPassword(id, dto.getPassword());
        return Result.ok();
    }

    @PutMapping("/{id}/roles")
    @SaCheckPermission("system:user:assignRole")
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody AssignRolesDTO dto) {
        userService.assignRoles(id, dto.getRoleIds());
        return Result.ok();
    }
}
