package com.pet.module.user.controller;

import com.pet.common.Result;
import com.pet.module.user.dto.UserVO;
import com.pet.module.user.entity.User;
import com.pet.module.user.service.UserService;
import com.pet.security.JwtAuthenticationToken;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理", description = "用户注册、登录、信息管理等功能")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    public Result<UserVO> getCurrentUser(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(userService.getUserInfo(token.getUserId()));
    }

    @PutMapping("/me")
    @Operation(summary = "更新当前用户信息", description = "更新当前登录用户的个人资料")
    public Result<UserVO> updateCurrentUser(@AuthenticationPrincipal JwtAuthenticationToken token,
                                            @RequestBody UserVO vo) {
        return Result.success(userService.updateUser(token.getUserId(), vo));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取所有用户列表", description = "管理员获取平台所有用户信息列表")
    public Result<List<UserVO>> listAll() {
        List<User> users = userService.listAll();
        List<UserVO> vos = users.stream().map(u -> {
            UserVO vo = new UserVO();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setNickname(u.getNickname());
            vo.setPhone(u.getPhone());
            vo.setAvatar(u.getAvatar());
            vo.setEmail(u.getEmail());
            vo.setStatus(u.getStatus());
            vo.setCreatedAt(u.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
        return Result.success(vos);
    }
}
