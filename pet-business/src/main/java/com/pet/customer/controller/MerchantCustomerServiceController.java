package com.pet.customer.controller;

import com.pet.common.Result;
import com.pet.customer.dto.MerchantCustomerServiceApplyRequestDTO;
import com.pet.customer.dto.MerchantCustomerServiceDTO;
import com.pet.customer.dto.MerchantCustomerServiceReviewRequestDTO;
import com.pet.customer.service.MerchantCustomerServiceService;
import com.pet.security.JwtAuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "【用户端】客服管理", description = "商家客服服务管理（用户申请/商家审核）")
@RestController
@RequestMapping("/api/merchant-customer-service")
@Slf4j
public class MerchantCustomerServiceController {
    private final MerchantCustomerServiceService service;

    public MerchantCustomerServiceController(MerchantCustomerServiceService service) {
        this.service = service;
    }

    /**
     * 申请成为客服
     *
     * <p>API: POST /api/merchant-customer-service/applications</p>
     * <p>请求来源：前端用户申请成为某商家客服页，用户填写申请信息后提交</p>
     * <p>权限要求：已登录用户（@PreAuthorize("isAuthenticated()")）</p>
     * <p>输入参数：@body MerchantCustomerServiceApplyRequestDTO - 包含商家ID、申请理由等</p>
     * <p>返回数据：MerchantCustomerServiceDTO - 客服申请记录，状态为PENDING待审核</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 参数校验失败或已提交过申请</li>
     *   <li>401 - 未登录</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @Operation(summary = "申请成为客服")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "申请提交成功"),
            @ApiResponse(responseCode = "401", description = "未认证")
    })
    @PostMapping("/applications")
    @PreAuthorize("isAuthenticated()")
    public Result<MerchantCustomerServiceDTO> apply(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Valid @RequestBody MerchantCustomerServiceApplyRequestDTO request) {
        return Result.success(service.apply(token.getUserId(), request));
    }

    /**
     * 查看我的客服申请列表
     *
     * <p>API: GET /api/merchant-customer-service/applications/me</p>
     * <p>请求来源：前端个人中心-我的客服申请页，查看自己提交的客服申请记录</p>
     * <p>权限要求：已登录用户（@PreAuthorize("isAuthenticated()")）</p>
     * <p>输入参数：无（从token中提取用户ID）</p>
     * <p>返回数据：List&lt;MerchantCustomerServiceDTO&gt; - 当前用户的所有客服申请记录</p>
     * <p>异常情况：
     * <ul>
     *   <li>401 - 未登录</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @Operation(summary = "查看我的客服申请列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未认证")
    })
    @GetMapping("/applications/me")
    @PreAuthorize("isAuthenticated()")
    public Result<List<MerchantCustomerServiceDTO>> listMine(
            @AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(service.listMine(token.getUserId()));
    }

    /**
     * 商家查看待审核的客服申请列表
     *
     * <p>API: GET /api/merchant-customer-service/merchant/applications/pending</p>
     * <p>请求来源：商家后台客服管理页，商家查看待审核的客服申请</p>
     * <p>权限要求：MERCHANT角色（@PreAuthorize("hasRole('MERCHANT')")）</p>
     * <p>输入参数：无（从token中提取用户ID，关联到商家）</p>
     * <p>返回数据：List&lt;MerchantCustomerServiceDTO&gt; - 归属该商家的待审核客服申请列表</p>
     * <p>异常情况：
     * <ul>
     *   <li>401 - 未登录</li>
     *   <li>403 - 无商家权限</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @Operation(summary = "商家查看待审核的客服申请列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "无商家权限")
    })
    @GetMapping("/merchant/applications/pending")
    @PreAuthorize("hasRole('MERCHANT')")
    public Result<List<MerchantCustomerServiceDTO>> listPendingForMerchant(
            @AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(service.listPendingForMerchant(token.getUserId()));
    }

    /**
     * 商家查看已通过审核的客服列表
     *
     * <p>API: GET /api/merchant-customer-service/merchant/staff</p>
     * <p>请求来源：商家后台客服管理页，商家查看已审核通过的客服人员</p>
     * <p>权限要求：MERCHANT角色（@PreAuthorize("hasRole('MERCHANT')")）</p>
     * <p>输入参数：无（从token中提取用户ID，关联到商家）</p>
     * <p>返回数据：List&lt;MerchantCustomerServiceDTO&gt; - 该商家已审核通过的客服员工列表</p>
     * <p>异常情况：
     * <ul>
     *   <li>401 - 未登录</li>
     *   <li>403 - 无商家权限</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @Operation(summary = "商家查看已通过审核的客服列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "无商家权限")
    })
    @GetMapping("/merchant/staff")
    @PreAuthorize("hasRole('MERCHANT')")
    public Result<List<MerchantCustomerServiceDTO>> listApprovedForMerchant(
            @AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(service.listApprovedForMerchant(token.getUserId()));
    }

    /**
     * 商家审核通过客服申请
     *
     * <p>API: POST /api/merchant-customer-service/merchant/applications/{id}/approve</p>
     * <p>请求来源：商家后台客服申请详情页，商家点击"审核通过"</p>
     * <p>权限要求：MERCHANT角色（@PreAuthorize("hasRole('MERCHANT')")）</p>
     * <p>输入参数：
     * <ul>
     *   <li>@path id - 客服申请记录ID</li>
     *   <li>@body MerchantCustomerServiceReviewRequestDTO - 可选，包含审核备注</li>
     * </ul>
     * </p>
     * <p>返回数据：MerchantCustomerServiceDTO - 审核通过后的客服记录，状态更新为APPROVED并自动分配CUSTOMER_SERVICE角色</p>
     * <p>异常情况：
     * <ul>
     *   <li>401 - 未登录</li>
     *   <li>403 - 无商家权限</li>
     *   <li>404 - 申请记录不存在</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @Operation(summary = "商家审核通过客服申请")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "审核通过成功"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "无商家权限"),
            @ApiResponse(responseCode = "404", description = "申请记录不存在")
    })
    @PostMapping("/merchant/applications/{id}/approve")
    @PreAuthorize("hasRole('MERCHANT')")
    public Result<MerchantCustomerServiceDTO> approve(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Parameter(description = "客服申请记录ID") @PathVariable Long id,
            @RequestBody(required = false) MerchantCustomerServiceReviewRequestDTO request) {
        return Result.success(service.approve(id, token.getUserId(), request));
    }

    /**
     * 商家驳回客服申请
     *
     * <p>API: POST /api/merchant-customer-service/merchant/applications/{id}/reject</p>
     * <p>请求来源：商家后台客服申请详情页，商家点击"驳回"</p>
     * <p>权限要求：MERCHANT角色（@PreAuthorize("hasRole('MERCHANT')")）</p>
     * <p>输入参数：
     * <ul>
     *   <li>@path id - 客服申请记录ID</li>
     *   <li>@body MerchantCustomerServiceReviewRequestDTO - 可选，包含驳回原因</li>
     * </ul>
     * </p>
     * <p>返回数据：MerchantCustomerServiceDTO - 驳回后的客服记录，状态更新为REJECTED</p>
     * <p>异常情况：
     * <ul>
     *   <li>401 - 未登录</li>
     *   <li>403 - 无商家权限</li>
     *   <li>404 - 申请记录不存在</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @Operation(summary = "商家驳回客服申请")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "驳回成功"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "无商家权限"),
            @ApiResponse(responseCode = "404", description = "申请记录不存在")
    })
    @PostMapping("/merchant/applications/{id}/reject")
    @PreAuthorize("hasRole('MERCHANT')")
    public Result<MerchantCustomerServiceDTO> reject(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Parameter(description = "客服申请记录ID") @PathVariable Long id,
            @RequestBody(required = false) MerchantCustomerServiceReviewRequestDTO request) {
        return Result.success(service.reject(id, token.getUserId(), request));
    }

    /**
     * 客服主动辞职
     *
     * <p>API: POST /api/merchant-customer-service/applications/{id}/resign</p>
     * <p>请求来源：客服端个人中心，客服主动申请辞职</p>
     * <p>权限要求：CUSTOMER_SERVICE角色（@PreAuthorize("hasRole('CUSTOMER_SERVICE')")）</p>
     * <p>输入参数：@path id - 客服申请记录ID</p>
     * <p>返回数据：MerchantCustomerServiceDTO - 辞职后的客服记录，状态更新为RESIGNED，自动移除CUSTOMER_SERVICE角色</p>
     * <p>异常情况：
     * <ul>
     *   <li>401 - 未登录</li>
     *   <li>403 - 无客服权限</li>
     *   <li>404 - 记录不存在</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @Operation(summary = "客服主动辞职")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "辞职成功"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "无客服权限"),
            @ApiResponse(responseCode = "404", description = "记录不存在")
    })
    @PostMapping("/applications/{id}/resign")
    @PreAuthorize("hasRole('CUSTOMER_SERVICE')")
    public Result<MerchantCustomerServiceDTO> resign(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Parameter(description = "客服申请记录ID") @PathVariable Long id) {
        return Result.success(service.resign(id, token.getUserId()));
    }

    /**
     * 商家终止客服关系
     *
     * <p>API: POST /api/merchant-customer-service/merchant/staff/{id}/terminate</p>
     * <p>请求来源：商家后台客服管理页，商家终止与客服的雇佣关系</p>
     * <p>权限要求：MERCHANT角色（@PreAuthorize("hasRole('MERCHANT')")）</p>
     * <p>输入参数：@path id - 客服记录ID</p>
     * <p>返回数据：MerchantCustomerServiceDTO - 终止后的客服记录，状态更新为TERMINATED，自动移除CUSTOMER_SERVICE角色</p>
     * <p>异常情况：
     * <ul>
     *   <li>401 - 未登录</li>
     *   <li>403 - 无商家权限</li>
     *   <li>404 - 记录不存在</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @Operation(summary = "商家终止客服关系")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "终止成功"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "无商家权限"),
            @ApiResponse(responseCode = "404", description = "记录不存在")
    })
    @PostMapping("/merchant/staff/{id}/terminate")
    @PreAuthorize("hasRole('MERCHANT')")
    public Result<MerchantCustomerServiceDTO> terminateByMerchant(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Parameter(description = "客服记录ID") @PathVariable Long id) {
        return Result.success(service.terminateByMerchant(id, token.getUserId()));
    }
}
