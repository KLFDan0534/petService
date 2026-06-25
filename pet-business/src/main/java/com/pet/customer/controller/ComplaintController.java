package com.pet.customer.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.PageParam;
import com.pet.common.PageResult;
import com.pet.common.Result;
import com.pet.customer.entity.Complaint;
import com.pet.customer.service.ComplaintService;
import com.pet.security.JwtAuthenticationToken;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/complaints")
@Tag(name = "投诉管理", description = "用户投诉管理")
@Slf4j
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    /**
     * 获取当前用户的投诉列表
     * @param token 当前用户认证信息
     * @return 投诉列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取我的投诉", description = "获取当前用户的投诉列表")
    public Result<List<Complaint>> listMyComplaints(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("调用 listMyComplaints()");
        return Result.success(complaintService.listByOwner(token.getUserId()));
    }

    /**
     * 管理员/客服分页查询所有投诉
     * @param pageParam 分页参数
     * @return 分页投诉列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_SERVICE')")
    @Operation(summary = "获取所有投诉列表", description = "管理员/客服分页查询所有投诉")
    public Result<PageResult<Complaint>> listAll(PageParam pageParam) {
        log.info("调用 listAll()");
        return Result.success(new PageResult<>(complaintService.listPage(pageParam)));
    }

    /**
     * 提交投诉
     * @param token 当前用户认证信息
     * @param complaint 投诉信息
     * @return 创建的投诉
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "提交投诉", description = "用户提交投诉")
    public Result<Complaint> create(@AuthenticationPrincipal JwtAuthenticationToken token,
                                    @Valid @RequestBody Complaint complaint) {
        log.info("调用 create()");
        return Result.success(complaintService.create(complaint));
    }

    /**
     * 管理员/客服标记投诉为已解决
     * @param id 投诉ID
     * @param body 请求体，包含result_wsh处理结果
     * @return 更新后的投诉
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_SERVICE')")
    @Operation(summary = "解决投诉", description = "管理员/客服标记投诉为已解决")
    public Result<Complaint> resolve(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.success(complaintService.process(id, body.get("result_wsh"), "resolved"));
    }

    /**
     * 管理员/客服驳回投诉
     * @param id 投诉ID
     * @param body 请求体，包含result_wsh驳回原因
     * @return 更新后的投诉
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_SERVICE')")
    @Operation(summary = "驳回投诉", description = "管理员/客服标记投诉为已驳回")
    public Result<Complaint> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.success(complaintService.process(id, body.get("result_wsh"), "rejected"));
    }
}
