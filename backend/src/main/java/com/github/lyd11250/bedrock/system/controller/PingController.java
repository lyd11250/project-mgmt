package com.github.lyd11250.bedrock.system.controller;

import com.github.lyd11250.bedrock.common.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查接口（开放，无需登录）：用于确认应用已成功启动。
 */
@Tag(name = "健康检查")
@RestController
@RequestMapping("/api/v1")
public class PingController {

    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.ok("pong");
    }
}
