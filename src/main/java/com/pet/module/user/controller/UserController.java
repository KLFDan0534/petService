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

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public Result<UserVO> getCurrentUser(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(userService.getUserInfo(token.getUserId()));
    }

    @PutMapping("/me")
    public Result<UserVO> updateCurrentUser(@AuthenticationPrincipal JwtAuthenticationToken token,
                                            @RequestBody UserVO vo) {
        return Result.success(userService.updateUser(token.getUserId(), vo));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
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
