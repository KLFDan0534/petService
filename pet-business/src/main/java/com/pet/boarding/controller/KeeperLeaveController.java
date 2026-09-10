package com.pet.boarding.controller;

import com.pet.boarding.dto.KeeperLeaveCreateRequestDTO;
import com.pet.boarding.dto.KeeperLeaveDTO;
import com.pet.boarding.service.KeeperLeaveService;
import com.pet.common.PageRequestDTO;
import com.pet.common.PageResult;
import com.pet.common.Result;
import com.pet.security.JwtAuthenticationToken;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
import java.util.stream.Collectors;

/**
 * 【请假管理控制器】
 *
 * 业务作用：
 * 商家为其旗下的看护者创建、查看和删除请假记录。
 * 请假期间看护者状态自动切换为 OFFLINE/BUSY（取决于业务配置）。
 *
 * 权限要求：
 * 所有接口仅 MERCHANT 角色可访问。
 *
 * API 路由前缀：/api/keeper-leaves
 */
@RestController
@RequestMapping("/api/keeper-leaves")
@Tag(name = "【用户端】请假管理", description = "看护者请假管理（商家查看和创建）")
@Slf4j
public class KeeperLeaveController {

    private final KeeperLeaveService leaveService;

    public KeeperLeaveController(KeeperLeaveService leaveService) {
        this.leaveService = leaveService;
    }

    /**
     * 商家获取请假列表
     *
     * <p>API: GET /api/keeper-leaves/merchant</p>
     * <p>请求来源：商家后台考勤管理页，商家查看其所有看护者的请假记录</p>
     * <p>权限要求：MERCHANT角色（@PreAuthorize("hasRole('MERCHANT')")）</p>
     * <p>输入参数：无（从token中提取用户ID，关联到商家）</p>
     * <p>返回数据：List&lt;KeeperLeaveDTO&gt; - 该商家名下所有看护者的请假记录</p>
     * <p>异常情况：
     * <ul>
     *   <li>403 - 非商家无权限</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    /**
     * 【获取商家请假列表】
     *
     * API: GET /api/keeper-leaves/merchant
     *
     * 权限：仅 MERCHANT
     *
     * 场景：商家在后台查看旗下所有看护者的请假记录，
     * 以便进行排班和人员调度。
     *
     * @param token 当前用户认证信息
     * @return 请假记录列表
     */
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

    /**
     * 商家创建请假
     *
     * <p>API: POST /api/keeper-leaves/merchant</p>
     * <p>请求来源：商家后台考勤管理页，商家为看护者添加请假记录</p>
     * <p>权限要求：MERCHANT角色（@PreAuthorize("hasRole('MERCHANT')")）</p>
     * <p>输入参数：@body KeeperLeaveCreateRequestDTO - 包含看护者ID、请假日期、原因等</p>
     * <p>返回数据：KeeperLeaveDTO - 创建的请假记录详情</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 参数校验失败（如看护者不存在、日期冲突）</li>
     *   <li>403 - 非商家无权限</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    /**
     * 【创建请假记录】
     *
     * API: POST /api/keeper-leaves/merchant
     *
     * 权限：仅 MERCHANT
     *
     * 场景：商家为旗下某看护者创建请假记录。
     *
     * 触发联动：请假生效期间看护者在线状态自动置为 OFFLINE，
     * 商家排班时不再将该看护者纳入可用人员。
     *
     * @param token   当前用户认证信息
     * @param request 请假信息（看护者 ID、开始/结束时间、请假类型等）
     * @return 创建的请假记录
     */
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

    /**
     * 商家删除请假记录
     *
     * <p>API: DELETE /api/keeper-leaves/merchant/{id}</p>
     * <p>请求来源：商家后台考勤管理页，商家删除指定的请假记录</p>
     * <p>权限要求：MERCHANT角色（@PreAuthorize("hasRole('MERCHANT')")）</p>
     * <p>输入参数：@path id - 请假记录ID</p>
     * <p>返回数据：无（Result.success()）</p>
     * <p>异常情况：
     * <ul>
     *   <li>403 - 非商家或无权限删除此记录</li>
     *   <li>404 - 请假记录不存在</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
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

    /**
     * 管理员分页查询全部请假记录
     *
     * <p>API: GET /api/keeper-leaves/admin-list</p>
     * <p>权限要求：ADMIN角色（@PreAuthorize("hasRole('ADMIN')")）</p>
     * <p>输入参数：分页参数（page/size），可选按审批状态精确过滤</p>
     * <p>返回数据：PageResult&lt;KeeperLeaveDTO&gt; - 请假记录分页（含看护者、商家名称）</p>
     */
    @GetMapping("/admin-list")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "分页查询全部请假记录", description = "管理员分页查看所有看护者的请假记录，可按审批状态过滤")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回请假记录分页"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<PageResult<KeeperLeaveDTO>> adminList(PageRequestDTO pageParam,
                                                        @RequestParam(required = false) String status_wsh) {
        log.info("调用 adminList(status_wsh={})", status_wsh);
        var page = leaveService.pageAll(pageParam, status_wsh);
        var dtoList = page.getRecords()
                .stream()
                .map(leaveService::toDTO)
                .collect(Collectors.toList());
        PageResult<KeeperLeaveDTO> result = new PageResult<>();
        result.setList(dtoList);
        result.copyPageInfo(page);
        return Result.success(result);
    }

    /**
     * 管理员审批通过请假申请
     *
     * <p>API: POST /api/keeper-leaves/{id}/approve</p>
     * <p>权限要求：ADMIN角色（@PreAuthorize("hasRole('ADMIN')")）</p>
     * <p>输入参数：@path id - 请假记录ID；@body 可选，Map 中可携带 reason 审批备注</p>
     * <p>返回数据：KeeperLeaveDTO - 审批后的请假记录（含看护者、商家名称）</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 请假记录非待审批状态</li>
     *   <li>404 - 请假记录不存在</li>
     * </ul>
     * </p>
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "审批通过请假", description = "管理员审批通过待审批的请假申请")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "审批通过成功"),
            @ApiResponse(responseCode = "400", description = "请假记录非待审批状态"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "请假记录不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<KeeperLeaveDTO> approve(@AuthenticationPrincipal JwtAuthenticationToken token,
                                          @Parameter(description = "请假记录ID") @PathVariable Long id,
                                          @RequestBody(required = false) Map<String, String> body) {
        log.info("调用 approve(id={})", id);
        String reason = body == null ? null : body.get("reason");
        return Result.success(leaveService.toDTO(leaveService.approve(id, token.getUserId(), reason)));
    }

    /**
     * 管理员驳回请假申请
     *
     * <p>API: POST /api/keeper-leaves/{id}/reject</p>
     * <p>权限要求：ADMIN角色（@PreAuthorize("hasRole('ADMIN')")）</p>
     * <p>输入参数：@path id - 请假记录ID；@body 可选，Map 中可携带 reason 驳回原因</p>
     * <p>返回数据：KeeperLeaveDTO - 驳回后的请假记录（含看护者、商家名称）</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 请假记录非待审批状态</li>
     *   <li>404 - 请假记录不存在</li>
     * </ul>
     * </p>
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "驳回请假", description = "管理员驳回待审批的请假申请")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "驳回成功"),
            @ApiResponse(responseCode = "400", description = "请假记录非待审批状态"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "请假记录不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<KeeperLeaveDTO> reject(@AuthenticationPrincipal JwtAuthenticationToken token,
                                         @Parameter(description = "请假记录ID") @PathVariable Long id,
                                         @RequestBody(required = false) Map<String, String> body) {
        log.info("调用 reject(id={})", id);
        String reason = body == null ? null : body.get("reason");
        return Result.success(leaveService.toDTO(leaveService.reject(id, token.getUserId(), reason)));
    }
}
