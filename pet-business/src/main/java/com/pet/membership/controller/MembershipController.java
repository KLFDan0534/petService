package com.pet.membership.controller;

import com.pet.common.Result;
import com.pet.membership.dto.MembershipDiscountDTO;
import com.pet.membership.dto.UserMembershipDTO;
import com.pet.membership.entity.MembershipBenefitUsage;
import com.pet.membership.service.MembershipBenefitService;
import com.pet.membership.service.MembershipService;
import com.pet.security.JwtAuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/membership")
@Tag(name = "【用户端】会员权益管理", description = "会员状态、权益报价和权益使用记录")
public class MembershipController {
    private final MembershipService membershipService;
    private final MembershipBenefitService membershipBenefitService;

    public MembershipController(MembershipService membershipService,
                                MembershipBenefitService membershipBenefitService) {
        this.membershipService = membershipService;
        this.membershipBenefitService = membershipBenefitService;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取我的会员状态", description = "获取当前用户会员状态、等级、权益和到期时间")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<UserMembershipDTO> getMyMembership(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(membershipService.getCurrentMembership(token.getUserId()));
    }

    @GetMapping("/benefits/order/quote")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "试算会员订单折扣", description = "按当前会员权益试算普通寄养订单会员折扣")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<MembershipDiscountDTO> quoteOrderDiscount(@AuthenticationPrincipal JwtAuthenticationToken token,
                                                            @Parameter(description = "待折扣金额")
                                                            @RequestParam BigDecimal amount_wsh) {
        return Result.success(membershipBenefitService.previewOrderDiscount(token.getUserId(), amount_wsh));
    }

    @GetMapping("/usages")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取我的会员权益使用记录", description = "获取当前用户会员权益抵扣、锁定和释放记录")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<MembershipBenefitUsage>> listMyUsages(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(membershipBenefitService.listUsageByUser(token.getUserId()));
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员获取用户会员状态", description = "管理员按状态查询用户会员记录")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<UserMembershipDTO>> listMembershipsForAdmin(@Parameter(description = "会员状态")
                                                                   @RequestParam(required = false) String status_wsh) {
        return Result.success(membershipService.listMembershipsForAdmin(status_wsh));
    }

    @GetMapping("/admin/usages")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员获取会员权益使用记录", description = "管理员查询会员权益使用流水")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<MembershipBenefitUsage>> listUsagesForAdmin(@Parameter(description = "用户ID")
                                                                   @RequestParam(required = false) Long user_id_wsh) {
        return Result.success(membershipBenefitService.listUsageForAdmin(user_id_wsh));
    }

    @PostMapping("/admin/users/expire")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "刷新过期会员", description = "管理员手动刷新已到期会员状态")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Integer> expireMemberships() {
        return Result.success(membershipService.expireMemberships());
    }
}
