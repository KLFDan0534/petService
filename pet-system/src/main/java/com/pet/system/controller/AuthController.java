package com.pet.system.controller;

import com.pet.common.Result;
import com.pet.system.dto.ForgotPasswordRequestDTO;
import com.pet.system.dto.LoginRequestDTO;
import com.pet.system.dto.RegisterCaptchaRequestDTO;
import com.pet.system.dto.RegisterRequestDTO;
import com.pet.system.dto.RefreshTokenRequestDTO;
import com.pet.system.service.TurnstileVerifyService;
import com.pet.system.service.UserService;
import com.pet.system.vo.LoginResponseVO;
import com.pet.system.vo.RegisterCaptchaVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
    private final TurnstileVerifyService turnstileVerifyService;

    public AuthController(UserService userService, TurnstileVerifyService turnstileVerifyService) {
        this.userService = userService;
        this.turnstileVerifyService = turnstileVerifyService;
    }

    /**
     * 用户注册
     *
     * <p>API: POST /api/auth/register</p>
     * <p>请求来源：前端注册页用户填写注册信息后提交</p>
     * <p>权限要求：无需登录，公开接口</p>
     * <p>输入参数：@body RegisterRequestDTO - 包含用户名、密码、手机号、邮箱、验证码等注册信息</p>
     * <p>返回数据：LoginResponseVO - 包含访问令牌、刷新令牌和用户基本信息，自动分配OWNER角色</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 参数校验失败（如用户名已存在、验证码错误、密码格式不符）</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @Operation(summary = "用户注册", description = "注册新用户，自动分配OWNER角色")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "注册成功，返回登录信息和用户信息"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/register")
    public Result<LoginResponseVO> register(@Valid @RequestBody RegisterRequestDTO request,
                                            HttpServletRequest httpRequest) {
        log.info("调用 register()");
        Result<Void> gate = turnstileGate(httpRequest, request.getTurnstileToken());
        if (gate != null) {
            return Result.error(gate.getCode(), gate.getErrorCode(), gate.getMessage());
        }
        return Result.success(userService.register(request));
    }

    /**
     * 获取注册验证码
     *
     * <p>API: POST /api/auth/register/captcha</p>
     * <p>请求来源：前端注册页用户点击"获取验证码"按钮</p>
     * <p>权限要求：无需登录，公开接口</p>
     * <p>输入参数：@body RegisterCaptchaRequestDTO - 包含手机号</p>
     * <p>返回数据：RegisterCaptchaVO - 包含验证码和过期时间（当前返回模拟验证码）</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 参数校验失败（如手机号格式错误）</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
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

    /**
     * 用户登录
     *
     * <p>API: POST /api/auth/login</p>
     * <p>请求来源：前端登录页用户输入凭据后点击登录</p>
     * <p>权限要求：无需登录，公开接口</p>
     * <p>输入参数：@body LoginRequestDTO - 包含用户名和密码</p>
     * <p>返回数据：LoginResponseVO - 包含AccessToken、RefreshToken和用户基本信息</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 参数校验失败或用户名/密码错误</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @Operation(summary = "用户登录", description = "使用用户名和密码登录，返回AccessToken和RefreshToken")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "登录成功，返回令牌和用户信息"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/login")
    public Result<LoginResponseVO> login(@Valid @RequestBody LoginRequestDTO request,
                                         HttpServletRequest httpRequest) {
        log.info("调用 login()");
        Result<Void> gate = turnstileGate(httpRequest, request.getTurnstileToken());
        if (gate != null) {
            return Result.error(gate.getCode(), gate.getErrorCode(), gate.getMessage());
        }
        return Result.success(userService.login(request));
    }

    /**
     * 刷新令牌
     *
     * <p>API: POST /api/auth/refresh</p>
     * <p>请求来源：前端检测到AccessToken即将过期时自动调用，或用户手动触发"刷新"操作</p>
     * <p>权限要求：无需登录，公开接口（需携带有效的RefreshToken）</p>
     * <p>输入参数：@body RefreshTokenRequestDTO - 包含refresh_token</p>
     * <p>返回数据：LoginResponseVO - 包含新的AccessToken、RefreshToken和用户基本信息</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - RefreshToken无效或已过期</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
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

    /**
     * 忘记密码 - 重置密码
     *
     * <p>API: POST /api/auth/forgot-password</p>
     * <p>请求来源：前端忘记密码页用户输入邮箱后点击"重置密码"</p>
     * <p>权限要求：无需登录，公开接口</p>
     * <p>输入参数：@body ForgotPasswordRequestDTO - 包含用户邮箱</p>
     * <p>返回数据：String - 临时密码，用户可使用此密码登录后修改</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 邮箱格式错误或该邮箱未注册</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @Operation(summary = "忘记密码", description = "通过邮箱重置密码，返回临时密码")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "重置成功，返回临时密码"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/forgot-password")
    public Result<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request,
                                         HttpServletRequest httpRequest) {
        log.info("调用 forgotPassword()");
        Result<Void> gate = turnstileGate(httpRequest, request.getTurnstileToken());
        if (gate != null) {
            return Result.error(gate.getCode(), gate.getErrorCode(), gate.getMessage());
        }
        String tempPassword = userService.forgotPassword(request.getEmail_wsh());
        return Result.success(tempPassword);
    }

    /**
     * 注销登录
     *
     * <p>API: POST /api/auth/logout</p>
     * <p>请求来源：前端用户点击"退出登录"按钮</p>
     * <p>权限要求：已登录用户</p>
     * <p>输入参数：@header Authorization - Bearer格式的AccessToken</p>
     * <p>返回数据：无（Result.success()）</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - Token格式错误或已失效</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
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

    /**
     * Turnstile 人机校验门禁。
     *
     * <p>校验失败（或未配置密钥）时返回错误 Result；通过时返回 null，由调用方继续执行业务逻辑。</p>
     */
    private Result<Void> turnstileGate(HttpServletRequest httpRequest, String token) {
        String userAgent = httpRequest.getHeader("User-Agent");
        if (!turnstileVerifyService.verify(token, clientIp(httpRequest), userAgent)) {
            log.warn("Turnstile 校验失败: {}", httpRequest.getRequestURI());
            return Result.error(400, "CAPTCHA_INVALID", "人机校验未通过，请完成验证后重试");
        }
        return null;
    }

    /**
     * 获取客户端真实 IP：优先取 X-Forwarded-For 首个地址（反代场景），否则回退到连接地址。
     */
    private String clientIp(HttpServletRequest httpRequest) {
        String forwarded = httpRequest.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return httpRequest.getRemoteAddr();
    }
}
