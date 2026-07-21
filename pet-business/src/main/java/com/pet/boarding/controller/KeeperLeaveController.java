package com.pet.boarding.controller;

import com.pet.boarding.dto.KeeperLeaveCreateRequestDTO;
import com.pet.boarding.dto.KeeperLeaveDTO;
import com.pet.boarding.service.KeeperLeaveService;
import com.pet.common.Result;
import com.pet.security.JwtAuthenticationToken;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/keeper-leaves")
@Tag(name = "【用户端】请假管理", description = "看护者请假管理（商家查看和创建）")
public class KeeperLeaveController {

    private final KeeperLeaveService leaveService;

    public KeeperLeaveController(KeeperLeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @GetMapping("/merchant")
    @PreAuthorize("hasRole('MERCHANT')")
    @Operation(summary = "获取商家请假列表", description = "商家查看所有看护者的请假列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回请假列表"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<KeeperLeaveDTO>> listByMerchant(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(leaveService.listByMerchant(token.getUserId()));
    }

    @PostMapping("/merchant")
    @PreAuthorize("hasRole('MERCHANT')")
    @Operation(summary = "创建请假", description = "商家为看护者创建请假记录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<KeeperLeaveDTO> createByMerchant(@AuthenticationPrincipal JwtAuthenticationToken token,
                                                   @Valid @RequestBody KeeperLeaveCreateRequestDTO request) {
        return Result.success(leaveService.createByMerchant(token.getUserId(), request));
    }

    @DeleteMapping("/merchant/{id}")
    @PreAuthorize("hasRole('MERCHANT')")
    @Operation(summary = "删除请假记录", description = "商家删除指定的请假记录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> deleteByMerchant(@AuthenticationPrincipal JwtAuthenticationToken token,
                                         @Parameter(description = "请假记录ID") @PathVariable Long id) {
        leaveService.deleteByMerchant(token.getUserId(), id);
        return Result.success();
    }
}
