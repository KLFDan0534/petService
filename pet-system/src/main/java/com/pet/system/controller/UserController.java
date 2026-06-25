package com.pet.system.controller;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.common.PageParam;
import com.pet.common.PageResult;
import com.pet.common.Result;
import com.pet.common.annotation.LogOperation;
import com.pet.system.vo.UserVO;
import com.pet.system.service.UserService;
import com.pet.security.JwtAuthenticationToken;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理", description = "用户注册、登录、信息管理")
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
    public Result<UserVO> updateCurrentUser(@AuthenticationPrincipal JwtAuthenticationToken token,
                                            @RequestBody UserVO vo) {
        log.info("调用 updateCurrentUser()");
        return Result.success(userService.updateUser(token.getUserId(), vo));
    }

    /**
     * 管理员更新用户状态（启用/禁用）
     * @param id 用户ID
     * @param body 请求体，包含status_wsh状态值
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @LogOperation(module = "user", operation = "update-status", description = "Update user status")
    @Operation(summary = "更新用户状态", description = "管理员启用或禁用用户")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        log.info("调用 updateStatus()");
        UserVO vo = new UserVO();
        vo.setStatus_wsh(body.get("status_wsh"));
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
    @Operation(summary = "分页查询用户列表", description = "管理员分页查询所有用户")
    public Result<PageResult<UserVO>> listAll(@Valid PageParam pageParam,
                                              @RequestParam(required = false) String q) {
        log.info("调用 listAll(), q={}", q);
        IPage<UserVO> up = userService.listPage(pageParam, q);
        PageResult<UserVO> pr = new PageResult<>();
        pr.copyPageInfo(up);
        pr.setList(up.getRecords());
        return Result.success(pr);
    }
}
