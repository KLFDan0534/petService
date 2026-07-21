package com.pet.system.controller;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.common.PageRequestDTO;
import com.pet.common.PageResult;
import com.pet.common.Result;
import com.pet.common.annotation.LogOperation;
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
     * @param token 当前用户认证信息
     * @return 当前用户信息
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
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
     * @param token 当前用户认证信息
     * @param vo 用户信息
     * @return 更新后的用户信息
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
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
     * @param token  当前用户认证信息
     * @param request  更新手机号请求体
     * @return  更新后的用户信息
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
        log.info("call updateCurrentUserPhone()");
        UserVO vo = new UserVO();
        vo.setPhone_wsh(request.getPhone_wsh());
        return Result.success(userService.updateUser(token.getUserId(), vo));
    }

    /**
     * 更新当前用户邮箱
     * @param token  当前用户认证信息
     * @param request  更新邮箱请求体
     * @return  更新后的用户信息
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
        log.info("call updateCurrentUserEmail()");
        UserVO vo = new UserVO();
        vo.setEmail_wsh(request.getEmail_wsh());
        return Result.success(userService.updateUser(token.getUserId(), vo));
    }

    /**
     * 提交实名认证信息
     * @param token  当前用户认证信息
     * @param request  提交实名认证信息请求体
     * @return  更新后的用户信息
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
        log.info("call updateCurrentUserRealName()");
        UserVO vo = new UserVO();
        vo.setReal_name_wsh(request.getReal_name_wsh());
        vo.setId_card_no_wsh(request.getId_card_no_wsh());
        vo.setReal_name_status_wsh(UserProfileConstants.REAL_NAME_PENDING);
        return Result.success(userService.updateUser(token.getUserId(), vo));
    }

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
        log.info("call setPaymentPassword()");
        userService.setPaymentPassword(token.getUserId(), request.getPayment_password_wsh());
        return Result.success();
    }

    /**
     * 管理员更新用户状态（启用/禁用）
     * @param id 用户ID
     * @param request 请求体，包含status_wsh状态值
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
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
     * @param pageParam 分页参数
     * @param q 搜索关键词（可选）
     * @return 分页用户列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
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
