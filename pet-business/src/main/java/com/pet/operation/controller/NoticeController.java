package com.pet.operation.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.BusinessException;
import com.pet.common.Result;
import com.pet.common.annotation.LogOperation;
import com.pet.operation.dto.NoticeCreateRequestDTO;
import com.pet.operation.dto.NoticeDTO;
import com.pet.operation.dto.NoticeUpdateRequestDTO;
import com.pet.operation.entity.Notice;
import com.pet.operation.service.NoticeService;
import com.pet.security.JwtAuthenticationToken;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/notices")
@Tag(name = "【用户端】公告管理", description = "系统公告和横幅管理（用户查看/管理员维护）")
@Slf4j
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    /**
     * 管理员获取所有公告列表
     * @param type 公告类型（可选）
     * @return 公告列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping
    @Operation(summary = "获取所有公告", description = "管理员获取所有公告列表")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<NoticeDTO>> listAll(@Parameter(description = "公告类型（可选）") @RequestParam(required = false) String type) {
        log.info("调用 listAll()");
        List<Notice> list = noticeService.listAll(type);
        return Result.success(list.stream().map(this::toDTO).collect(Collectors.toList()));
    }

    /**
     * 获取当前生效的公告和Banner列表
     * @param type 公告类型（可选）
     * @return 生效公告列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/active")
    @Operation(summary = "获取生效公告", description = "获取当前生效的公告和Banner列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<NoticeDTO>> listActive(@Parameter(description = "公告类型（可选）") @RequestParam(required = false) String type) {
        log.info("调用 listActive()");
        List<Notice> list = noticeService.listActive(type);
        return Result.success(list.stream().map(this::toDTO).collect(Collectors.toList()));
    }

    /**
     * 获取当前用户未读的公告列表
     * @param token 当前用户认证信息
     * @return 未读公告列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/unread")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取未读公告", description = "获取当前用户未读的公告列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<NoticeDTO>> listUnread(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("调用 listUnread()");
        List<Notice> list = noticeService.listUnread(token.getUserId());
        return Result.success(list.stream().map(this::toDTO).collect(Collectors.toList()));
    }

    /**
     * 管理员创建公告或Banner
     * @param request 公告信息
     * @return 创建的公告
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @LogOperation(module = "notice", operation = "create", description = "新增公告")
    @Operation(summary = "新增公告", description = "管理员创建公告或Banner")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<NoticeDTO> create(@Valid @RequestBody NoticeCreateRequestDTO request) {
        log.info("调用 create()");
        return Result.success(toDTO(noticeService.create(request)));
    }

    /**
     * 管理员更新公告
     * @param id 公告ID
     * @param request 公告信息
     * @return 更新后的公告
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @LogOperation(module = "notice", operation = "update", description = "更新公告")
    @Operation(summary = "更新公告", description = "管理员更新公告")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<NoticeDTO> update(@Parameter(description = "公告ID") @PathVariable Long id, @Valid @RequestBody NoticeUpdateRequestDTO request) {
        log.info("调用 update()");
        return Result.success(toDTO(noticeService.update(id, request)));
    }

    /**
     * 根据ID获取公告详情
     * @param id 公告ID
     * @return 公告详情
     * @author: wsh
     * @date: 2026/7/1 11:00
     **/
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取公告详情", description = "根据ID获取公告详情")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<NoticeDTO> getById(@Parameter(description = "公告ID") @PathVariable Long id) {
        log.info("调用 getById()");
        try {
            return Result.success(toDTO(noticeService.getById(id)));
        } catch (BusinessException e) {
            return Result.error(404, e.getMessage());
        }
    }

    /**
     * 获取当前用户未读的弹窗公告
     * @param token 当前用户认证信息
     * @return 未读弹窗列表
     * @author: wsh
     * @date: 2026/7/1 11:00
     **/
    @GetMapping("/popup")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取未读弹窗", description = "获取当前用户未读的弹窗公告")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<NoticeDTO>> listPopup(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("调用 listPopup()");
        List<Notice> list = noticeService.listPopup(token.getUserId());
        return Result.success(list.stream().map(this::toDTO).collect(Collectors.toList()));
    }

    /**
     * 当前用户关闭弹窗公告
     * @param token 当前用户认证信息
     * @param id 公告ID
     * @return 无返回值
     * @author: wsh
     * @date: 2026/7/1 11:00
     **/
    @PostMapping("/{id}/dismiss-popup")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "关闭弹窗", description = "当前用户关闭弹窗公告")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> dismissPopup(@AuthenticationPrincipal JwtAuthenticationToken token,
                                     @Parameter(description = "公告ID") @PathVariable Long id) {
        log.info("调用 dismissPopup()");
        noticeService.dismissPopup(id, token.getUserId());
        return Result.success();
    }

    /**
     * 当前用户标记公告为已读
     * @param token 当前用户认证信息
     * @param id 公告ID
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "标记公告已读", description = "当前用户确认阅读公告")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> markAsRead(@AuthenticationPrincipal JwtAuthenticationToken token,
                                   @Parameter(description = "公告ID") @PathVariable Long id) {
        log.info("调用 markAsRead()");
        noticeService.markAsRead(id, token.getUserId());
        return Result.success();
    }

    /**
     * 管理员删除公告
     * @param id 公告ID
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @LogOperation(module = "notice", operation = "delete", description = "删除公告")
    @Operation(summary = "删除公告", description = "管理员删除公告")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> delete(@Parameter(description = "公告ID") @PathVariable Long id) {
        log.info("调用 delete()");
        noticeService.delete(id);
        return Result.success();
    }

    private NoticeDTO toDTO(Notice entity) {
        NoticeDTO dto = new NoticeDTO();
        dto.setId_wsh(entity.getId_wsh());
        dto.setTitle_wsh(entity.getTitle_wsh());
        dto.setContent_wsh(entity.getContent_wsh());
        dto.setType_wsh(entity.getType_wsh());
        dto.setDelivery_type_wsh(entity.getDelivery_type_wsh());
        dto.setImage_url_wsh(entity.getImage_url_wsh());
        dto.setLink_url_wsh(entity.getLink_url_wsh());
        dto.setSort_order_wsh(entity.getSort_order_wsh());
        dto.setStatus_wsh(entity.getStatus_wsh());
        dto.setCreated_at_wsh(entity.getCreated_at_wsh());
        return dto;
    }
}
