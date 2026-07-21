package com.pet.operation.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.operation.dto.NotificationDTO;
import com.pet.operation.dto.UnreadCountResponseDTO;
import com.pet.operation.entity.Notification;
import com.pet.operation.service.NotificationService;
import com.pet.security.JwtAuthenticationToken;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "【用户端】通知管理", description = "用户通知管理")
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<NotificationDTO>> list(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("list() called");
        List<Notification> list = notificationService.listByUser(token.getUserId());
        return Result.success(list.stream().map(this::toDTO).collect(Collectors.toList()));
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<UnreadCountResponseDTO> unreadCount(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("unreadCount() called");
        long count = notificationService.countUnread(token.getUserId());
        return Result.success(new UnreadCountResponseDTO(count));
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> markAsRead(@AuthenticationPrincipal JwtAuthenticationToken token,
                                   @Parameter(description = "通知ID") @PathVariable Long id) {
        log.info("markAsRead() called");
        notificationService.markAsRead(id,token.getUserId());
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> markAllAsRead(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("markAllAsRead() called");
        notificationService.markAllAsRead(token.getUserId());
        return Result.success();
    }

    private NotificationDTO toDTO(Notification entity) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId_wsh(entity.getId_wsh());
        dto.setUser_id_wsh(entity.getUser_id_wsh());
        dto.setTitle_wsh(entity.getTitle_wsh());
        dto.setContent_wsh(entity.getContent_wsh());
        dto.setType_wsh(entity.getType_wsh());
        dto.setIs_read_wsh(entity.getIs_read_wsh());
        dto.setRelated_id_wsh(entity.getRelated_id_wsh());
        dto.setCreated_at_wsh(entity.getCreated_at_wsh());
        return dto;
    }
}
