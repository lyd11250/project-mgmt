package com.github.lyd11250.auth.controller;

import com.github.lyd11250.auth.dto.LoginDTO;
import com.github.lyd11250.auth.service.AuthService;
import com.github.lyd11250.auth.vo.LoginVO;
import com.github.lyd11250.auth.vo.MeVO;
import com.github.lyd11250.common.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口。{@code /login} 开放，其余需登录。
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<MeVO> me() {
        return Result.ok(authService.me());
    }
}
