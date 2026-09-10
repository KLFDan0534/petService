package com.pet.system.controller;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.common.PageRequestDTO;
import com.pet.common.PageResult;
import com.pet.common.Result;
import com.pet.common.annotation.LogOperation;
import com.pet.system.dto.AdminResetAllPasswordsRequestDTO;
import com.pet.system.dto.UpdateUserStatusRequestDTO;
import com.pet.system.dto.UserPaymentPasswordRequestDTO;
import com.pet.system.dto.UserUpdateEmailRequestDTO;
import com.pet.system.dto.UserUpdatePhoneRequestDTO;
import com.pet.system.dto.UserUpdateRealNameRequestDTO;
import com.pet.system.profile.UserProfileConstants;
import com.pet.system.vo.UserVO;
import com.pet.system.service.UserService;
import com.pet.security.JwtAuthenticationToken;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/users")
@Tag(name = "【用户端】用户管理", description = "用户信息管理、实名认证、账号操作")
@Slf4j
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 获取当前登录用户信息
     *
     * <p>API: GET /api/users/me</p>
     * <p>请求来源：前端个人中心页、系统全局（用于获取当前用户信息渲染界面）</p>
     * <p>权限要求：已登录用户（isAuthenticated）</p>
     * <p>输入参数：无path/query参数，token中提取userId</p>
     * <p>返回数据：UserVO - 包含用户ID、用户名、昵称、头像、手机号、邮箱、实名状态、角色列表等</p>
     * <p>异常情况：
     * <ul>
     *   <li>401 - 未登录或Token无效</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<UserVO> getCurrentUser(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("调用 getCurrentUser()");
        return Result.success(userService.getUserInfo(token.getUserId()));
    }

    /**
     * 更新当前用户个人信息
     *
     * <p>API: PUT /api/users/me</p>
     * <p>请求来源：前端个人设置页，用户修改昵称、头像等个人信息后保存</p>
     * <p>权限要求：已登录用户（isAuthenticated）</p>
     * <p>输入参数：@body UserVO - 包含要更新的字段（昵称、头像URL、个性签名等）</p>
     * <p>返回数据：UserVO - 更新后的用户完整信息</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 参数校验失败</li>
     *   <li>401 - 未登录或Token无效</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @PutMapping("/me")
    @LogOperation(module = "user", operation = "update", description = "Update personal info")
    @Operation(summary = "更新当前用户信息", description = "更新当前登录用户的个人信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<UserVO> updateCurrentUser(@AuthenticationPrincipal JwtAuthenticationToken token,
                                            @RequestBody UserVO vo) {
        log.info("调用 updateCurrentUser()");
        return Result.success(userService.updateUser(token.getUserId(), vo));
    }

    /**
     * 更新当前用户手机号
     *
     * <p>API: PUT /api/users/me/phone</p>
     * <p>请求来源：前端安全设置页，用户修改绑定手机号</p>
     * <p>权限要求：已登录用户（isAuthenticated）</p>
     * <p>输入参数：@body UserUpdatePhoneRequestDTO - 包含新手机号</p>
     * <p>返回数据：UserVO - 更新手机号后的用户完整信息</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 手机号格式错误或已被其他账号绑定</li>
     *   <li>401 - 未登录或Token无效</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @PutMapping("/me/phone")
    @LogOperation(module = "user", operation = "update-phone", description = "Update bound phone")
    @Operation(summary = "更新手机号", description = "更新当前用户的手机号")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<UserVO> updateCurrentUserPhone(@AuthenticationPrincipal JwtAuthenticationToken token,
                                                 @Valid @RequestBody UserUpdatePhoneRequestDTO request) {
        log.info("调用 updateCurrentUserPhone()");
        UserVO vo = new UserVO();
        vo.setPhone_wsh(request.getPhone_wsh());
        return Result.success(userService.updateUser(token.getUserId(), vo));
    }

    /**
     * 更新当前用户邮箱
     *
     * <p>API: PUT /api/users/me/email</p>
     * <p>请求来源：前端安全设置页，用户修改绑定邮箱</p>
     * <p>权限要求：已登录用户（isAuthenticated）</p>
     * <p>输入参数：@body UserUpdateEmailRequestDTO - 包含新邮箱地址</p>
     * <p>返回数据：UserVO - 更新邮箱后的用户完整信息</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 邮箱格式错误或已被其他账号使用</li>
     *   <li>401 - 未登录或Token无效</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @PutMapping("/me/email")
    @LogOperation(module = "user", operation = "update-email", description = "Update email")
    @Operation(summary = "更新邮箱", description = "更新当前用户的邮箱")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<UserVO> updateCurrentUserEmail(@AuthenticationPrincipal JwtAuthenticationToken token,
                                                 @Valid @RequestBody UserUpdateEmailRequestDTO request) {
        log.info("调用 updateCurrentUserEmail()");
        UserVO vo = new UserVO();
        vo.setEmail_wsh(request.getEmail_wsh());
        return Result.success(userService.updateUser(token.getUserId(), vo));
    }

    /**
     * 提交实名认证信息
     *
     * <p>API: PUT /api/users/me/real-name</p>
     * <p>请求来源：前端实名认证页，用户填写真实姓名和身份证号后提交</p>
     * <p>权限要求：已登录用户（isAuthenticated）</p>
     * <p>输入参数：@body UserUpdateRealNameRequestDTO - 包含真实姓名和身份证号</p>
     * <p>返回数据：UserVO - 提交实名后的用户信息，real_name_status_wsh变为PENDING待审核状态</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 姓名或身份证号格式错误</li>
     *   <li>401 - 未登录或Token无效</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @PutMapping("/me/real-name")
    @LogOperation(module = "user", operation = "update-real-name", description = "Submit real-name verification")
    @Operation(summary = "提交实名认证", description = "提交当前用户的实名认证信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<UserVO> updateCurrentUserRealName(@AuthenticationPrincipal JwtAuthenticationToken token,
                                                    @Valid @RequestBody UserUpdateRealNameRequestDTO request) {
        log.info("调用 updateCurrentUserRealName()");
        UserVO vo = new UserVO();
        vo.setReal_name_wsh(request.getReal_name_wsh());
        vo.setId_card_no_wsh(request.getId_card_no_wsh());
        vo.setReal_name_status_wsh(UserProfileConstants.REAL_NAME_PENDING);
        return Result.success(userService.updateUser(token.getUserId(), vo));
    }

    /**
     * 设置或重置支付密码
     *
     * <p>API: PUT /api/users/me/payment-password</p>
     * <p>请求来源：前端安全设置页，用户设置或修改支付密码</p>
     * <p>权限要求：已登录用户（isAuthenticated）</p>
     * <p>输入参数：@body UserPaymentPasswordRequestDTO - 包含支付密码明文</p>
     * <p>返回数据：无（Result.success()），密码经过加密后存储在用户记录中</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 密码格式不满足安全要求</li>
     *   <li>401 - 未登录或Token无效</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @PutMapping("/me/payment-password")
    @LogOperation(module = "user", operation = "set-payment-password", description = "Set payment password")
    @Operation(summary = "设置支付密码", description = "设置或重置当前用户的支付密码")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> setPaymentPassword(@AuthenticationPrincipal JwtAuthenticationToken token,
                                           @Valid @RequestBody UserPaymentPasswordRequestDTO request) {
        log.info("调用 setPaymentPassword()");
        userService.setPaymentPassword(token.getUserId(), request.getPayment_password_wsh());
        return Result.success();
    }

    /**
     * 注销当前登录用户账号
     *
     * <p>API: DELETE /api/users/me</p>
     * <p>请求来源：前端账号安全页，用户点击"注销账号"</p>
     * <p>权限要求：已登录用户（isAuthenticated）</p>
     * <p>输入参数：@header Authorization - Bearer格式的AccessToken</p>
     * <p>返回数据：无（Result.success()），用户账号被标记为已删除，Token立即失效</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 用户存在未完成订单等原因无法注销</li>
     *   <li>401 - 未登录或Token无效</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @DeleteMapping("/me")
    @LogOperation(module = "user", operation = "delete", description = "Delete current account")
    @Operation(summary = "注销账号", description = "注销当前登录用户账号")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> deleteCurrentUser(@AuthenticationPrincipal JwtAuthenticationToken token,
                                          @RequestHeader("Authorization") String authHeader) {
        log.info("调用 deleteCurrentUser()");
        String accessToken = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        userService.deleteCurrentUser(token.getUserId(), accessToken);
        return Result.success();
    }

    /**
     * 管理员批量重置用户登录密码
     *
     * <p>API: PUT /api/users/passwords/reset-all</p>
     * <p>请求来源：后台安全管理页，管理员需要统一修复或轮换用户登录凭据时调用</p>
     * <p>权限要求：ADMIN角色（@PreAuthorize("hasRole('ADMIN')")）</p>
     * <p>输入参数：@body AdminResetAllPasswordsRequestDTO - 包含新的登录密码</p>
     * <p>返回数据：Integer - 实际更新的用户记录数</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 密码为空或长度不满足6到72个字符</li>
     *   <li>403 - 非管理员无权限</li>
     *   <li>500 - 数据库更新失败</li>
     * </ul>
     * </p>
     * <p>注意事项：服务层会使用BCrypt加密密码后再写入数据库，接口不会返回密码或密码摘要</p>
     */
    @PutMapping("/passwords/reset-all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "批量重置登录密码", description = "管理员将所有用户的登录密码重置为同一BCrypt密码")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "重置成功，返回更新用户数量"),
        @ApiResponse(responseCode = "400", description = "密码格式错误"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<Integer> resetAllPasswords(@Valid @RequestBody AdminResetAllPasswordsRequestDTO request) {
        log.info("调用 resetAllPasswords()");
        return Result.success(userService.resetAllPasswords(request.getPassword_wsh()));
    }

    /**
     * 管理员更新用户状态（启用/禁用）
     *
     * <p>API: PUT /api/users/{id}/status</p>
     * <p>请求来源：后台管理用户列表页，管理员启用或禁用用户账号</p>
     * <p>权限要求：ADMIN角色（@PreAuthorize("hasRole('ADMIN')")）</p>
     * <p>输入参数：@path id - 用户ID；@body UpdateUserStatusRequestDTO - 包含status_wsh状态值</p>
     * <p>返回数据：无（Result.success()）</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 无效的状态值或用户不存在</li>
     *   <li>403 - 非管理员无权限</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @LogOperation(module = "user", operation = "update-status", description = "Update user status")
    @Operation(summary = "更新用户状态", description = "管理员启用或禁用用户")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> updateStatus(@Parameter(description = "用户ID") @PathVariable Long id, @Valid @RequestBody UpdateUserStatusRequestDTO request) {
        log.info("调用 updateStatus()");
        UserVO vo = new UserVO();
        vo.setStatus_wsh(request.getStatus_wsh());
        userService.updateUser(id, vo);
        return Result.success();
    }

    /**
     * 管理员分页查询所有用户
     *
     * <p>API: GET /api/users</p>
     * <p>请求来源：后台管理用户列表页，支持按关键词搜索和分页</p>
     * <p>权限要求：ADMIN角色（@PreAuthorize("hasRole('ADMIN')")）</p>
     * <p>输入参数：
     * <ul>
     *   <li>@query PageRequestDTO - 分页参数（页码、每页大小）</li>
     *   <li>@query q - 搜索关键词（可选），模糊匹配用户名/昵称/手机号</li>
     * </ul>
     * </p>
     * <p>返回数据：PageResult&lt;UserVO&gt; - 分页用户列表，包含用户基本信息、角色、状态等</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 分页参数错误</li>
     *   <li>403 - 非管理员无权限</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "分页查询用户列表", description = "管理员分页查询所有用户，可按关键词搜索")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功，返回分页用户列表"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<PageResult<UserVO>> listAll(@Valid PageRequestDTO pageParam,
                                               @Parameter(description = "搜索关键词（用户名/昵称/手机号）") @RequestParam(required = false) String q) {
        log.info("调用 listAll(), q={}", q);
        IPage<UserVO> up = userService.listPage(pageParam, q);
        PageResult<UserVO> pr = new PageResult<>();
        pr.copyPageInfo(up);
        pr.setList(up.getRecords());
        return Result.success(pr);
    }
}
