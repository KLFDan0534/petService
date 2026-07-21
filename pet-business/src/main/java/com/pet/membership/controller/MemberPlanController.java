package com.pet.membership.controller;

import com.pet.common.Result;
import com.pet.membership.dto.MemberPlanCreateRequestDTO;
import com.pet.membership.dto.MemberPlanDTO;
import com.pet.membership.dto.MemberPlanStatusRequestDTO;
import com.pet.membership.dto.MemberPlanUpdateRequestDTO;
import com.pet.membership.service.MemberPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/membership")
@Tag(name = "【后台管理】会员套餐管理", description = "会员套餐配置管理（用户查看/管理员维护）")
public class MemberPlanController {
    private final MemberPlanService memberPlanService;

    public MemberPlanController(MemberPlanService memberPlanService) {
        this.memberPlanService = memberPlanService;
    }

    @GetMapping("/plans/active")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取启用会员套餐", description = "获取当前可购买的启用会员套餐")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<MemberPlanDTO>> listActivePlans() {
        return Result.success(memberPlanService.listPlans(null, true));
    }

    @GetMapping("/admin/plans")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员获取会员套餐", description = "管理员获取启用和停用的会员套餐列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<MemberPlanDTO>> listPlans(@Parameter(description = "套餐状态") @RequestParam(required = false) Integer status_wsh) {
        return Result.success(memberPlanService.listPlans(status_wsh, false));
    }

    @GetMapping("/admin/plans/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员获取会员套餐详情", description = "根据ID获取会员套餐详情")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<MemberPlanDTO> getPlan(@Parameter(description = "套餐ID") @PathVariable Long id) {
        return Result.success(memberPlanService.getPlan(id));
    }

    @PostMapping("/admin/plans")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "新增会员套餐", description = "管理员新增会员套餐")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<MemberPlanDTO> createPlan(@Valid @RequestBody MemberPlanCreateRequestDTO request) {
        return Result.success(memberPlanService.createPlan(request));
    }

    @PutMapping("/admin/plans/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新会员套餐", description = "管理员更新会员套餐")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<MemberPlanDTO> updatePlan(@Parameter(description = "套餐ID") @PathVariable Long id,
                                            @Valid @RequestBody MemberPlanUpdateRequestDTO request) {
        return Result.success(memberPlanService.updatePlan(id, request));
    }

    @PostMapping("/admin/plans/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新会员套餐状态", description = "管理员启用或停用会员套餐")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<MemberPlanDTO> updateStatus(@Parameter(description = "套餐ID") @PathVariable Long id,
                                              @Valid @RequestBody MemberPlanStatusRequestDTO request) {
        return Result.success(memberPlanService.updateStatus(id, request.getStatus_wsh()));
    }

    @DeleteMapping("/admin/plans/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "删除会员套餐", description = "管理员删除未被会员或会员订单引用的套餐")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> deletePlan(@Parameter(description = "套餐ID") @PathVariable Long id) {
        memberPlanService.deletePlan(id);
        return Result.success();
    }
}
