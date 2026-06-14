package com.pet.module.complaint.controller;

import com.pet.common.Result;
import com.pet.module.complaint.entity.Complaint;
import com.pet.module.complaint.service.ComplaintService;
import com.pet.security.JwtAuthenticationToken;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/complaints")
@Tag(name = "投诉管理", description = "用户投诉的提交、处理和管理")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @GetMapping
    @Operation(summary = "获取我的投诉列表", description = "获取当前用户的所有投诉记录")
    public Result<List<Complaint>> listMyComplaints(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(complaintService.listByOwner(token.getUserId()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_SERVICE')")
    @Operation(summary = "获取全部投诉列表", description = "管理员或客服获取所有投诉记录")
    public Result<List<Complaint>> listAll() {
        return Result.success(complaintService.listAll());
    }

    @PostMapping
    @Operation(summary = "提交投诉", description = "用户提交新的投诉")
    public Result<Complaint> create(@AuthenticationPrincipal JwtAuthenticationToken token,
                                     @RequestBody Complaint complaint) {
        complaint.setOwnerId(token.getUserId());
        return Result.success(complaintService.create(complaint));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_SERVICE')")
    @Operation(summary = "解决投诉", description = "管理员或客服处理并解决投诉")
    public Result<Complaint> resolve(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.success(complaintService.process(id, body.get("result"), "resolved"));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_SERVICE')")
    @Operation(summary = "驳回投诉", description = "管理员或客服驳回投诉")
    public Result<Complaint> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.success(complaintService.process(id, body.get("result"), "rejected"));
    }
}
