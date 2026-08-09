package com.pet.membership.controller;

import com.pet.common.Result;
import com.pet.membership.dto.MembershipOrderCreateRequestDTO;
import com.pet.membership.dto.MembershipOrderDTO;
import com.pet.membership.service.MembershipOrderService;
import com.pet.security.JwtAuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/membership/orders")
@Tag(name = "【用户端】会员订单管理", description = "会员购买和续费订单管理（创建待支付订单/查询/取消）")
public class MembershipOrderController {
    private final MembershipOrderService membershipOrderService;

    public MembershipOrderController(MembershipOrderService membershipOrderService) {
        this.membershipOrderService = membershipOrderService;
    }

    /**
     * 创建会员订单
     *
     * <p>API: POST /api/membership/orders</p>
     * <p>请求来源：前端会员中心-购买会员页，用户选择套餐后点击"立即购买"</p>
     * <p>权限要求：已登录用户（@PreAuthorize("isAuthenticated()")）</p>
     * <p>输入参数：@body MembershipOrderCreateRequestDTO - 包含会员套餐planId</p>
     * <p>返回数据：MembershipOrderDTO - 创建的待支付会员订单（不执行扣款和开通）</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 套餐不存在或已停用</li>
     *   <li>401 - 未登录</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "创建会员订单", description = "基于启用会员套餐创建待支付会员订单，不执行扣款和开通")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<MembershipOrderDTO> create(@AuthenticationPrincipal JwtAuthenticationToken token,
                                              @Valid @RequestBody MembershipOrderCreateRequestDTO request) {
        return Result.success(membershipOrderService.createOrder(token.getUserId(), request));
    }

    /**
     * 获取我的会员订单
     *
     * <p>API: GET /api/membership/orders</p>
     * <p>请求来源：前端会员中心-我的订单页，查看自己的会员购买和续费记录</p>
     * <p>权限要求：已登录用户（@PreAuthorize("isAuthenticated()")）</p>
     * <p>输入参数：无（从token中提取用户ID）</p>
     * <p>返回数据：List&lt;MembershipOrderDTO&gt; - 当前用户的会员订单列表</p>
     * <p>异常情况：
     * <ul>
     *   <li>401 - 未登录</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取我的会员订单", description = "获取当前用户的会员购买和续费订单")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<MembershipOrderDTO>> listMyOrders(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(membershipOrderService.listMyOrders(token.getUserId()));
    }

    /**
     * 获取会员订单详情
     *
     * <p>API: GET /api/membership/orders/{orderNo}</p>
     * <p>请求来源：前端会员中心-订单详情页，查看指定会员订单的详细信息</p>
     * <p>权限要求：已登录用户（@PreAuthorize("isAuthenticated()")）</p>
     * <p>输入参数：@path orderNo - 会员订单号</p>
     * <p>返回数据：MembershipOrderDTO - 会员订单详情</p>
     * <p>异常情况：
     * <ul>
     *   <li>401 - 未登录</li>
     *   <li>403 - 无权限（非本人订单）</li>
     *   <li>404 - 订单不存在</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @GetMapping("/{orderNo}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取会员订单详情", description = "根据会员订单号获取当前用户自己的会员订单")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<MembershipOrderDTO> getMyOrder(@AuthenticationPrincipal JwtAuthenticationToken token,
                                                  @Parameter(description = "会员订单号") @PathVariable String orderNo) {
        return Result.success(membershipOrderService.getMyOrder(token.getUserId(), orderNo));
    }

    /**
     * 取消会员订单
     *
     * <p>API: POST /api/membership/orders/{orderNo}/cancel</p>
     * <p>请求来源：前端会员中心-我的订单页，用户取消待支付的会员订单</p>
     * <p>权限要求：已登录用户（@PreAuthorize("isAuthenticated()")）</p>
     * <p>输入参数：@path orderNo - 会员订单号</p>
     * <p>返回数据：MembershipOrderDTO - 取消后的会员订单</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 订单状态不允许取消（非待支付状态）</li>
     *   <li>401 - 未登录</li>
     *   <li>403 - 无权限（非本人订单）</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @PostMapping("/{orderNo}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "取消会员订单", description = "取消当前用户自己的待支付会员订单")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<MembershipOrderDTO> cancel(@AuthenticationPrincipal JwtAuthenticationToken token,
                                              @Parameter(description = "会员订单号") @PathVariable String orderNo) {
        return Result.success(membershipOrderService.cancelPendingOrder(token.getUserId(), orderNo));
    }

    /**
     * 支付会员订单
     *
     * <p>API: POST /api/membership/orders/{orderNo}/pay</p>
     * <p>请求来源：前端会员中心-待支付订单页，用户点击"立即支付"</p>
     * <p>权限要求：已登录用户（@PreAuthorize("isAuthenticated()")）</p>
     * <p>输入参数：@path orderNo - 会员订单号</p>
     * <p>返回数据：MembershipOrderDTO - 支付后的会员订单，会员已激活或续费成功（余额支付方式）</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 订单状态异常、余额不足或支付密码错误</li>
     *   <li>401 - 未登录</li>
     *   <li>403 - 无权限（非本人订单）</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @PostMapping("/{orderNo}/pay")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "支付会员订单", description = "使用余额支付当前用户自己的待支付会员订单，并激活或续费会员；外部支付方式需等待平台确认")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<MembershipOrderDTO> pay(@AuthenticationPrincipal JwtAuthenticationToken token,
                                           @Parameter(description = "会员订单号") @PathVariable String orderNo) {
        return Result.success(membershipOrderService.payOrder(token.getUserId(), orderNo));
    }

    /**
     * 管理员获取会员订单
     *
     * <p>API: GET /api/membership/orders/admin/list</p>
     * <p>请求来源：后台管理会员订单页，管理员按状态查询所有会员购买和续费订单</p>
     * <p>权限要求：ADMIN角色（@PreAuthorize("hasRole('ADMIN')")）</p>
     * <p>输入参数：@query status_wsh - 订单状态筛选（可选）</p>
     * <p>返回数据：List&lt;MembershipOrderDTO&gt; - 匹配状态的会员订单列表</p>
     * <p>异常情况：
     * <ul>
     *   <li>403 - 非管理员无权限</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员获取会员订单", description = "管理员按状态查询会员购买和续费订单")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<MembershipOrderDTO>> listOrdersForAdmin(@Parameter(description = "会员订单状态")
                                                                @RequestParam(required = false) String status_wsh) {
        return Result.success(membershipOrderService.listOrdersForAdmin(status_wsh));
    }

    /**
     * 管理员确认会员订单支付
     *
     * <p>API: POST /api/membership/orders/admin/{orderNo}/pay</p>
     * <p>请求来源：后台管理会员订单页，管理员手动确认外部支付方式的订单支付成功</p>
     * <p>权限要求：ADMIN角色（@PreAuthorize("hasRole('ADMIN')")）</p>
     * <p>输入参数：@path orderNo - 会员订单号</p>
     * <p>返回数据：MembershipOrderDTO - 确认支付后的会员订单，会员已激活或续费</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 订单状态异常</li>
     *   <li>403 - 非管理员无权限</li>
     *   <li>404 - 订单不存在</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @PostMapping("/admin/{orderNo}/pay")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员确认会员订单支付", description = "管理员手动确认会员订单支付成功，并激活或续费会员")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<MembershipOrderDTO> confirmPaidForAdmin(@AuthenticationPrincipal JwtAuthenticationToken token,
                                                           @Parameter(description = "会员订单号") @PathVariable String orderNo) {
        return Result.success(membershipOrderService.confirmPaidForAdmin(token.getUserId(), orderNo));
    }
}
