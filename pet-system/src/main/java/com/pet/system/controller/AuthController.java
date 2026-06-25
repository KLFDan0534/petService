package com.pet.system.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.system.dto.LoginRequest;
import com.pet.system.dto.LoginResponse;
import com.pet.system.dto.RegisterRequest;
import com.pet.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "认证管理", description = "用户注册、登录、令牌刷新、注销")
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 用户注册，自动分配OWNER角色
     * @param request 注册请求信息
     * @return 登录响应（包含token）
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @Operation(summary = "用户注册", description = "注册新用户，自动分配OWNER角色")
    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("调用 register()");
        return Result.success(userService.register(request));
    }

    /**
     * 用户登录
     * @param request 登录请求（用户名/密码）
     * @return 登录响应（包含AccessToken和RefreshToken）
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @Operation(summary = "用户登录", description = "使用用户名/密码登录，返回AccessToken和RefreshToken")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("调用 login()");
        return Result.success(userService.login(request));
    }

    /**
     * 刷新AccessToken
     * @param body 请求体，包含refresh_token_wsh
     * @return 新的登录响应（包含新token）
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @Operation(summary = "刷新令牌", description = "使用RefreshToken获取新的AccessToken")
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@RequestBody Map<String, String> body) {
        log.info("调用 refresh()");
        return Result.success(userService.refreshToken(body.get("refresh_token_wsh")));
    }

    /**
     * 忘记密码，通过邮箱重置密码
     * @param body 请求体，包含email_wsh
     * @return 临时密码
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @Operation(summary = "忘记密码", description = "通过邮箱重置密码，返回临时密码")
    @PostMapping("/forgot-password")
    public Result<String> forgotPassword(@RequestBody Map<String, String> body) {
        log.info("调用 forgotPassword()");
        String tempPassword = userService.forgotPassword(body.get("email_wsh"));
        return Result.success(tempPassword);
    }

    /**
     * 用户注销，使当前token失效
     * @param authHeader Authorization请求头
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @Operation(summary = "注销", description = "使当前令牌失效")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader) {
        log.info("调用 logout()");
        String token = authHeader.replace("Bearer ", "");
        userService.logout(token);
        return Result.success();
    }
}
