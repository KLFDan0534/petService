package com.pet.customer.controller;

import com.pet.common.PageResult;
import com.pet.common.Result;
import com.pet.customer.dto.ComplaintCreateRequestDTO;
import com.pet.customer.dto.ComplaintDTO;
import com.pet.customer.dto.ComplaintEvidenceDTO;
import com.pet.customer.dto.ComplaintListRequestDTO;
import com.pet.customer.dto.ComplaintMessageDTO;
import com.pet.customer.dto.ComplaintMessageSendRequestDTO;
import com.pet.customer.dto.ComplaintReviewRequestDTO;
import com.pet.customer.dto.ComplaintTargetsDTO;
import com.pet.customer.service.ComplaintService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
@Tag(name = "【用户端】投诉管理", description = "投诉管理（用户投诉/管理员处理）")
public class ComplaintController {

    private final ComplaintService complaintService;

    /**
     * 业务作用：注入投诉业务服务，供各 REST 接口调用。
     * 业务规则：依赖为 final 字段。
     */
    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    /**
     * 业务作用：返回当前登录用户提交的全部投诉。
     * 业务规则：需登录（isAuthenticated）。
     * 异常情况：未登录由 Spring Security 拦截返回 401。
     * 注意事项：只能看到本人投诉，不区分状态。
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取我的投诉列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<ComplaintDTO>> listMyComplaints(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(complaintService.listByOwner(token.getUserId()));
    }

    /**
     * 业务作用：返回当前用户可投诉的对象（订单、消费过的商家、服务过的寄养师）。
     * 调用场景：用户新建投诉时前端渲染选择列表，避免手填 ID。
     * 业务规则：需登录。
     * 异常情况：未登录返回 401。
     * 注意事项：只返回与当前用户有实际交易关系的对象。
     */
    @GetMapping("/targets")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取可投诉目标列表", description = "返回当前用户可投诉/举报的对象（订单、消费过的商家、服务过的寄养师），前端渲染为选择列表，用户无需手填ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ComplaintTargetsDTO> targets(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(complaintService.getTargets(token.getUserId()));
    }

    /**
     * 业务作用：管理员/商家/客服分页查询投诉，按角色限定可见范围。
     * 业务规则：仅 ADMIN / MERCHANT / CUSTOMER_SERVICE 可访问。
     * 异常情况：无权限由 @PreAuthorize 拦截返回 403。
     * 注意事项：三种角色可同时成立，服务层按角色组合计算可见范围。
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT','CUSTOMER_SERVICE')")
    @Operation(summary = "管理员获取所有投诉列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<PageResult<ComplaintDTO>> listAll(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            ComplaintListRequestDTO pageParam) {
        // 角色标记一并下传，可见范围由服务层收敛
        return Result.success(new PageResult<>(complaintService.listPageForStaff(
                pageParam,
                token.getUserId(),
                hasRole(token, "ADMIN"),
                hasRole(token, "MERCHANT"),
                hasRole(token, "CUSTOMER_SERVICE"))));
    }

    /**
     * 业务作用：查询指定投诉的举证材料。
     * 业务规则：仅内部人员（ADMIN / MERCHANT / CUSTOMER_SERVICE）可查看。
     * 异常情况：投诉不存在抛 404；越权由服务层抛 403。
     * 注意事项：可见范围同样依赖角色组合，由服务层判定。
     */
    @GetMapping("/{id}/evidence")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT','CUSTOMER_SERVICE')")
    @Operation(summary = "获取投诉证据")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "资源不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ComplaintEvidenceDTO> evidence(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @PathVariable @Parameter(description = "投诉ID") Long id) {
        return Result.success(complaintService.getEvidenceForStaff(
                id,
                token.getUserId(),
                hasRole(token, "ADMIN"),
                hasRole(token, "MERCHANT"),
                hasRole(token, "CUSTOMER_SERVICE")));
    }

    /**
     * 业务作用：当前登录用户提交一条新投诉。
     * 业务规则：需登录；请求体字段受 Bean Validation 约束。
     * 异常情况：参数不合法抛 400；未登录返回 401。
     * 注意事项：投诉人身份取自 token，不接受请求体传入。
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "创建投诉")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ComplaintDTO> create(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Valid @RequestBody ComplaintCreateRequestDTO request) {
        return Result.success(complaintService.create(request, token.getUserId()));
    }

    /**
     * 业务作用：查看某条投诉下的往来沟通消息。
     * 业务规则：投诉人本人或可管理该投诉的内部人员才可查看。
     * 异常情况：投诉不存在抛 404；无权限抛 403。
     * 注意事项：权限判定完全在服务层完成。
     */
    @GetMapping("/{id}/messages")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "查询投诉沟通消息", description = "投诉人本人或可管理该投诉的内部人员查看往来消息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "资源不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<ComplaintMessageDTO>> listMessages(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @PathVariable @Parameter(description = "投诉ID") Long id) {
        return Result.success(complaintService.listMessages(
                id,
                token.getUserId(),
                hasRole(token, "ADMIN"),
                hasRole(token, "MERCHANT"),
                hasRole(token, "CUSTOMER_SERVICE")));
    }

    /**
     * 业务作用：在投诉会话中发送一条消息，并站内通知接收方。
     * 业务规则：需登录；请求体经 @Valid 校验。
     * 异常情况：参数不合法抛 400；投诉不存在抛 404；越权抛 403。
     * 注意事项：发送人身份取自 token，不接受请求体传入。
     */
    @PostMapping("/{id}/messages")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "发送投诉沟通消息", description = "用户追问或客服回复，站内通知接收方")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "资源不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ComplaintMessageDTO> sendMessage(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @PathVariable @Parameter(description = "投诉ID") Long id,
            @Valid @RequestBody ComplaintMessageSendRequestDTO request) {
        return Result.success(complaintService.sendMessage(
                id,
                token.getUserId(),
                request.getContent_wsh(),
                request.getFile_url_wsh(),
                hasRole(token, "ADMIN"),
                hasRole(token, "MERCHANT"),
                hasRole(token, "CUSTOMER_SERVICE")));
    }

    /**
     * 业务作用：把投诉从 pending 推进到 processing，表示已有人接手。
     * 业务规则：仅内部人员可受理；状态机只允许 pending → processing。
     * 异常情况：状态不允许时由服务层抛 400；投诉不存在抛 404。
     * 注意事项：受理人身份由 token 决定，服务层会记录处理人。
     */
    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT','CUSTOMER_SERVICE')")
    @Operation(summary = "受理投诉", description = "受理投诉进入处理中状态(pending -> processing)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "资源不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ComplaintDTO> accept(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @PathVariable @Parameter(description = "投诉ID") Long id) {
        return Result.success(complaintService.accept(
                id,
                token.getUserId(),
                hasRole(token, "ADMIN"),
                hasRole(token, "MERCHANT"),
                hasRole(token, "CUSTOMER_SERVICE")));
    }

    /**
     * 业务作用：把投诉置为已解决，并写入处理结论。
     * 业务规则：仅内部人员可操作；结论经 @Valid 校验。
     * 异常情况：投诉不存在抛 404；越权抛 403；状态不允许由服务层抛 400。
     * 注意事项：与 reject 共用 processForStaff，仅状态字面量不同。
     */
    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT','CUSTOMER_SERVICE')")
    @Operation(summary = "处理投诉")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "资源不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ComplaintDTO> resolve(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @PathVariable @Parameter(description = "投诉ID") Long id,
            @Valid @RequestBody ComplaintReviewRequestDTO request) {
        return Result.success(complaintService.processForStaff(
                id,
                request.getResult_wsh(),
                "resolved",
                token.getUserId(),
                hasRole(token, "ADMIN"),
                hasRole(token, "MERCHANT"),
                hasRole(token, "CUSTOMER_SERVICE")));
    }

    /**
     * 业务作用：把投诉置为已驳回，并写入驳回理由。
     * 业务规则：仅内部人员可操作。
     * 异常情况：投诉不存在抛 404；越权抛 403；状态不允许由服务层抛 400。
     * 注意事项：与 resolve 共用 processForStaff，仅状态字面量不同。
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT','CUSTOMER_SERVICE')")
    @Operation(summary = "驳回投诉")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "资源不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ComplaintDTO> reject(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @PathVariable @Parameter(description = "投诉ID") Long id,
            @Valid @RequestBody ComplaintReviewRequestDTO request) {
        return Result.success(complaintService.processForStaff(
                id,
                request.getResult_wsh(),
                "rejected",
                token.getUserId(),
                hasRole(token, "ADMIN"),
                hasRole(token, "MERCHANT"),
                hasRole(token, "CUSTOMER_SERVICE")));
    }

    /**
     * 业务作用：检查 token 的权限列表中是否包含指定角色。
     * 业务规则：token 为 null 时返回 false。
     * 注意事项：角色名传参不含 ROLE_ 前缀，方法内部自动补齐。
     */
    private boolean hasRole(JwtAuthenticationToken token, String role) {
        String authority = "ROLE_" + role;
        return token != null
                && token.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }
}
