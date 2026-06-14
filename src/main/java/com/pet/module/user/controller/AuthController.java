package com.pet.module.user.controller;

import com.pet.common.Result;
import com.pet.module.user.dto.LoginRequest;
import com.pet.module.user.dto.LoginResponse;
import com.pet.module.user.dto.RegisterRequest;
import com.pet.module.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "认证管理", description = "用户注册、登录、刷新令牌、退出登录")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "用户注册", description = "注册新用户，自动分配OWNER角色")
    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(userService.register(request));
    }

    @Operation(summary = "用户登录", description = "用户名密码登录，返回AccessToken和RefreshToken")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(userService.login(request));
    }

    @Operation(summary = "刷新令牌", description = "使用RefreshToken获取新的AccessToken")
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@RequestBody Map<String, String> body) {
        return Result.success(userService.refreshToken(body.get("refreshToken")));
    }

    @Operation(summary = "退出登录", description = "使当前令牌失效")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        userService.logout(token);
        return Result.success();
    }
}
