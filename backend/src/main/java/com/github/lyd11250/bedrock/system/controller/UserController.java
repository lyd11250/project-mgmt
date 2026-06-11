package com.github.lyd11250.bedrock.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.lyd11250.bedrock.system.dto.AssignRolesDTO;
import com.github.lyd11250.bedrock.system.dto.ChangePasswordDTO;
import com.github.lyd11250.bedrock.system.dto.ResetPasswordDTO;
import com.github.lyd11250.bedrock.system.dto.UpdateProfileDTO;
import com.github.lyd11250.bedrock.system.dto.UserCreateDTO;
import com.github.lyd11250.bedrock.system.dto.UserUpdateDTO;
import com.github.lyd11250.bedrock.system.service.FileService;
import com.github.lyd11250.bedrock.system.service.UserService;
import com.github.lyd11250.bedrock.system.vo.ProfileVO;
import com.github.lyd11250.bedrock.system.vo.UserVO;
import com.github.lyd11250.bedrock.common.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户管理接口（租户内，按权限校验）。
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/v1/system/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ---- 自助（当前登录用户，登录即可，无需额外权限）----

    @GetMapping("/me/profile")
    public Result<ProfileVO> profile() {
        return Result.ok(userService.getProfile());
    }

    @PutMapping("/me/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UpdateProfileDTO dto) {
        userService.updateProfile(dto);
        return Result.ok();
    }

    @PutMapping("/me/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        userService.changeOwnPassword(dto);
        return Result.ok();
    }

    /** 上传/更换当前登录用户头像，返回新头像文件 id。 */
    @PutMapping("/me/avatar")
    public Result<Long> updateAvatar(@RequestParam("file") MultipartFile file) {
        return Result.ok(userService.updateAvatar(file));
    }

    /** 读取指定用户头像（本租户登录可见）；内联返回图片流，供 &lt;img&gt; 显示。 */
    @GetMapping("/{id}/avatar")
    public ResponseEntity<InputStreamResource> avatar(@PathVariable Long id) {
        FileService.DownloadFile d = userService.getAvatar(id);
        MediaType mediaType = d.contentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(d.contentType());
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentType(mediaType);
        if (d.size() >= 0) {
            builder.contentLength(d.size());
        }
        return builder.body(new InputStreamResource(d.content()));
    }

    // ---- 管理（按权限校验）----

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
