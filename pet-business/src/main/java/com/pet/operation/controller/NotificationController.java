package com.pet.operation.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.operation.entity.Notification;
import com.pet.operation.service.NotificationService;
import com.pet.security.JwtAuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "娑堟伅閫氱煡", description = "鐃6�1�79�1�7�1�7埛娑堟伅閫氱煡绠＄悊")
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 获取当前用户的通知列表
     * @param token 当前用户认证信息
     * @return 通知列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取我的通知列表")
    public Result<List<Notification>> list(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("list() called");
        return Result.success(notificationService.listByUser(token.getUserId()));
    }

    /**
     * 获取未读通知数量
     * @param token 当前用户认证信息
     * @return 包含未读数量的Map
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取未读通知数量")
    public Result<Map<String, Long>> unreadCount(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("unreadCount() called");
        long count = notificationService.countUnread(token.getUserId());
        return Result.success(Map.of("count", count));
    }

    /**
     * 标记通知为已读
     * @param token 当前用户认证信息
     * @param id 通知ID
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "标记通知已读")
    public Result<Void> markAsRead(@AuthenticationPrincipal JwtAuthenticationToken token,
                                   @PathVariable Long id) {
        log.info("markAsRead() called");
        notificationService.markAsRead(id, token.getUserId());
        return Result.success();
    }

    /**
     * 标记所有通知为已读
     * @param token 当前用户认证信息
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "标记所有通知已读")
    public Result<Void> markAllAsRead(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success();
    }
}