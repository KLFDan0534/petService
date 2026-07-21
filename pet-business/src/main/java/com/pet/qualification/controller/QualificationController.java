package com.pet.qualification.controller;

import com.pet.common.Result;
import com.pet.qualification.dto.QualificationDTO;
import com.pet.qualification.service.QualificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/qualifications")
@Tag(name = "【用户端】资质管理", description = "商家和看护者资质审核管理")
public class QualificationController {
    private final QualificationService qualificationService;

    public QualificationController(QualificationService qualificationService) {
        this.qualificationService = qualificationService;
    }

    @GetMapping("/{ownerType}/{ownerId}")
    @Operation(summary = "获取公开资质列表", description = "根据拥有者类型和ID获取公开的资质列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回资质列表"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<QualificationDTO>> listPublic(@Parameter(description = "拥有者类型") @PathVariable String ownerType,
                                                     @Parameter(description = "拥有者ID") @PathVariable Long ownerId) {
        return Result.success(qualificationService.listByOwner(ownerType, ownerId, true));
    }

    @GetMapping("/pending")
    @Operation(summary = "获取待审核资质", description = "获取所有待审核的资质申请列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回待审核列表"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<QualificationDTO>> listPending() {
        return Result.success(qualificationService.listPending());
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审核通过资质", description = "审核通过指定的资质申请")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "审核通过返回更新后的资质信息"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<QualificationDTO> approve(@Parameter(description = "资质ID") @PathVariable Long id,
                                            @Parameter(description = "审核人ID") @RequestParam Long reviewerId) {
        return Result.success(qualificationService.approve(id, reviewerId));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "驳回资质申请", description = "驳回指定的资质申请并填写备注")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "驳回成功返回更新后的资质信息"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<QualificationDTO> reject(@Parameter(description = "资质ID") @PathVariable Long id,
                                           @Parameter(description = "审核人ID") @RequestParam Long reviewerId,
                                           @Parameter(description = "驳回备注") @RequestParam(required = false) String remark) {
        return Result.success(qualificationService.reject(id, reviewerId, remark));
    }
}
