package com.pet.operation.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.common.annotation.LogOperation;
import com.pet.operation.entity.Notice;
import com.pet.operation.service.NoticeService;
import com.pet.security.JwtAuthenticationToken;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/notices")
@Tag(name = "公告管理", description = "系统公告和横幅管理")
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
    public Result<List<Notice>> listAll(@RequestParam(required = false) String type) {
        log.info("调用 listAll()");
        return Result.success(noticeService.listAll(type));
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
    public Result<List<Notice>> listActive(@RequestParam(required = false) String type) {
        log.info("调用 listActive()");
        return Result.success(noticeService.listActive(type));
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
    public Result<List<Notice>> listUnread(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("调用 listUnread()");
        return Result.success(noticeService.listUnread(token.getUserId()));
    }

    /**
     * 管理员创建公告或Banner
     * @param notice 公告信息
     * @return 创建的公告
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @LogOperation(module = "notice", operation = "create", description = "新增公告")
    @Operation(summary = "新增公告", description = "管理员创建公告或Banner")
    public Result<Notice> create(@Valid @RequestBody Notice notice) {
        log.info("调用 create()");
        return Result.success(noticeService.create(notice));
    }

    /**
     * 管理员更新公告
     * @param id 公告ID
     * @param notice 公告信息
     * @return 更新后的公告
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @LogOperation(module = "notice", operation = "update", description = "更新公告")
    @Operation(summary = "更新公告", description = "管理员更新公告")
    public Result<Notice> update(@PathVariable Long id, @Valid @RequestBody Notice notice) {
        log.info("调用 update()");
        return Result.success(noticeService.update(id, notice));
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
    public Result<Void> markAsRead(@AuthenticationPrincipal JwtAuthenticationToken token,
                                   @PathVariable Long id) {
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
    public Result<Void> delete(@PathVariable Long id) {
        log.info("调用 delete()");
        noticeService.delete(id);
        return Result.success();
    }
}
