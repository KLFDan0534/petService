package com.pet.system.controller;

import com.pet.common.Result;
import com.pet.system.dto.ForgotPasswordRequestDTO;
import com.pet.system.dto.LoginRequestDTO;
import com.pet.system.dto.RegisterCaptchaRequestDTO;
import com.pet.system.dto.RegisterRequestDTO;
import com.pet.system.dto.RefreshTokenRequestDTO;
import com.pet.system.service.UserService;
import com.pet.system.vo.LoginResponseVO;
import com.pet.system.vo.RegisterCaptchaVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "【公共】认证管理", description = "用户注册、登录、刷新令牌和注销")
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "用户注册", description = "注册新用户，自动分配OWNER角色")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "注册成功，返回登录信息和用户信息"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/register")
    public Result<LoginResponseVO> register(@Valid @RequestBody RegisterRequestDTO request) {
        log.info("调用 register()");
        return Result.success(userService.register(request));
    }

    @Operation(summary = "获取注册验证码", description = "获取注册验证码，当前返回模拟验证码")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功，返回验证码和过期时间"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/register/captcha")
    public Result<RegisterCaptchaVO> requestRegisterCaptcha(@Valid @RequestBody RegisterCaptchaRequestDTO request) {
        log.info("调用 requestRegisterCaptcha()");
        return Result.success(userService.requestRegisterCaptcha(request.getPhone_wsh()));
    }

    @Operation(summary = "用户登录", description = "使用用户名和密码登录，返回AccessToken和RefreshToken")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "登录成功，返回令牌和用户信息"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/login")
    public Result<LoginResponseVO> login(@Valid @RequestBody LoginRequestDTO request) {
        log.info("调用 login()");
        return Result.success(userService.login(request));
    }

    @Operation(summary = "刷新令牌", description = "使用RefreshToken获取新的AccessToken")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "刷新成功，返回新的令牌和用户信息"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/refresh")
    public Result<LoginResponseVO> refresh(@Valid @RequestBody RefreshTokenRequestDTO request) {
        log.info("调用 refresh()");
        return Result.success(userService.refreshToken(request.getRefresh_token_wsh()));
    }

    @Operation(summary = "忘记密码", description = "通过邮箱重置密码，返回临时密码")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "重置成功，返回临时密码"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/forgot-password")
    public Result<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        log.info("调用 forgotPassword()");
        String tempPassword = userService.forgotPassword(request.getEmail_wsh());
        return Result.success(tempPassword);
    }

    @Operation(summary = "注销登录", description = "使当前令牌失效")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "注销成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader) {
        log.info("调用 logout()");
        String token = authHeader.replace("Bearer ", "");
        userService.logout(token);
        return Result.success();
    }
}
