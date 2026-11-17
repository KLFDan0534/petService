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

    /**
     * 【构造方法】注入会员服务与权益服务
     *
     * 业务作用：由 Spring 注入 MembershipService 与 MembershipBenefitService。
     * 异常情况：依赖缺失时由 Spring 抛出 Bean 创建异常。
     * 注意事项：依赖为 final 字段。
     */
    public MembershipController(MembershipService membershipService,
                                MembershipBenefitService membershipBenefitService) {
        this.membershipService = membershipService;
        this.membershipBenefitService = membershipBenefitService;
    }

    /**
     * 【获取我的会员状态】返回当前用户会员等级、权益与到期时间
     *
     * 业务作用：对外暴露 GET /api/membership/me。
     * 业务规则：仅需登录（isAuthenticated）。
     * 异常情况：未登录 401；服务层查询异常向上传播。
     */
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

    /**
     * 【试算会员订单折扣】按会员权益计算订单可减免金额
     *
     * 业务作用：对外暴露 GET /api/membership/benefits/order/quote，下单前试算折扣。
     * 业务规则：仅需登录；金额参数缺失时由 Spring 抛 400。
     * 异常情况：未登录 401；参数缺失或类型不符 400；服务层业务校验失败抛业务异常。
     * 注意事项：仅试算，实际抵扣需在下单流程中由权益服务真正锁定额度。
     */
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

    /**
     * 【获取我的权益使用记录】返回当前用户的抵扣、锁定与释放流水
     *
     * 业务作用：对外暴露 GET /api/membership/usages。
     * 业务规则：仅需登录；只能查自己的记录。
     * 异常情况：未登录 401。
     * 注意事项：直接返回实体 MembershipBenefitUsage 而非 DTO，实体字段变更会直接影响接口契约。
     */
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

    /**
     * 【管理员查会员记录】按状态筛选用户会员记录
     *
     * 业务作用：对外暴露 GET /api/membership/admin/users。
     * 业务规则：仅 ADMIN（hasRole ADMIN）。
     * 异常情况：非管理员 403；状态值非法时由服务层校验。
     * 注意事项：接口无分页，会员量大时返回体可能很大。
     */
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

    /**
     * 【管理员查权益流水】按用户筛选权益使用记录
     *
     * 业务作用：对外暴露 GET /api/membership/admin/usages。
     * 业务规则：仅 ADMIN。
     * 异常情况：非管理员 403。
     * 注意事项：不传 user_id_wsh 时会查全表，数据量大时有性能风险；返回实体而非 DTO。
     */
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

    /**
     * 【刷新过期会员】手动把已到期会员置为过期
     *
     * 业务作用：对外暴露 POST /api/membership/admin/users/expire，供管理员手动触发过期处理。
     * 业务规则：仅 ADMIN。
     * 异常情况：非管理员 403；执行异常向上传播。
     * 注意事项：通常由定时任务自动执行，此接口为人工兜底，重复调用应幂等。
     */
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
