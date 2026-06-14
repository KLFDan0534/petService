package com.pet.module.user.controller;

import com.pet.common.Result;
import com.pet.module.user.dto.LoginRequest;
import com.pet.module.user.dto.LoginResponse;
import com.pet.module.user.dto.RegisterRequest;
import com.pet.module.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(userService.register(request));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(userService.login(request));
    }

    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@RequestBody Map<String, String> body) {
        return Result.success(userService.refreshToken(body.get("refreshToken")));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        userService.logout(token);
        return Result.success();
    }
}
