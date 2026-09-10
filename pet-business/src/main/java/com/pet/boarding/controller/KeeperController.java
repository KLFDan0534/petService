package com.pet.boarding.controller;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.Result;
import com.pet.boarding.dto.KeeperCreateRequestDTO;
import com.pet.boarding.dto.KeeperUpdateRequestDTO;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.security.JwtAuthenticationToken;
import com.pet.boarding.service.KeeperService;
import com.pet.boarding.service.MerchantService;
import com.pet.boarding.vo.KeeperVO;
import com.pet.common.BusinessException;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import com.pet.boarding.dto.KeeperOnlineStatusRequestDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 【看护者管理控制器】
 *
 * 业务作用：
 * 管理看护者的全生命周期 API，包括查看、搜索、注册、审核、
 * 上下级关联（商户-看护者）、在线状态切换、辞职/终止关系。
 *
 * 权限要求：
 * - 公开接口（GET）：任意用户可查看
 * - 创建/更新：需登录（isAuthenticated）
 * - 管理员审核（approve/reject/online-status）：仅 ADMIN
 * - 商户审核（merchant-approve/merchant-reject/terminate）：仅 MERCHANT
 * - 看护者自身操作（resign）：仅 KEEPER
 *
 * API 路由前缀：/api/keepers
 */
@RestController
@RequestMapping("/api/keepers")
@Tag(name = "【用户端】看护者管理", description = "看护者信息管理、注册、审核及在线状态管理（用户/商家/管理员使用）")
@Slf4j
public class KeeperController {

    private final KeeperService keeperService;
    private final KeeperMapper keeperMapper;
    private final MerchantService merchantService;

    public KeeperController(KeeperService keeperService, KeeperMapper keeperMapper, MerchantService merchantService) {
        this.keeperService = keeperService;
        this.keeperMapper = keeperMapper;
        this.merchantService = merchantService;
    }

    /**
     * 【获取所有已审核看护者列表】
     *
     * API: GET /api/keepers
     *
     * 权限：公开（无需登录）
     *
     * 场景：用户在首页浏览所有已审核通过的看护者。
     *
     * 返回：仅返回 status = KEEPER_APPROVED 的看护者。
     */
    @GetMapping
    @Operation(summary = "获取所有已审核看护者", description = "获取平台上所有已审核通过的看护者列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<KeeperVO>> listAll() {
        log.info("调用 listAll()");
        return Result.success(keeperService.listAll().stream().map(keeperService::toDTO).collect(Collectors.toList()));
    }

    /**
     * 【管理员获取待审核看护者列表】
     *
     * API: GET /api/keepers/pending
     *
     * 权限：仅 ADMIN
     *
     * 场景：管理员在后台查看所有待审核的看护者入驻申请，
     * 进行批量审核操作。
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取待审核看护者", description = "管理员获取所有待审核的看护者申请")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<KeeperVO>> listPending() {
        log.info("调用 listPending()");
        return Result.success(keeperService.listPending().stream().map(keeperService::toDTO).collect(Collectors.toList()));
    }

    /**
     * 【获取当前登录用户的看护者信息】
     *
     * API: GET /api/keepers/me
     *
     * 权限：需登录
     *
     * 场景：看护者登录后在个人信息页查看自己的信息。
     * 如果当前用户不是看护者，返回 null。
     *
     * @param token 当前用户认证信息
     * @return 看护者信息，非看护者用户返回 null
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取当前看护者信息", description = "根据当前登录用户获取看护者信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<KeeperVO> getCurrentKeeper(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("调用 getCurrentKeeper()");
        var qw = new LambdaQueryWrapper<com.pet.boarding.entity.Keeper>()
                .eq(com.pet.boarding.entity.Keeper::getUser_id_wsh, token.getUserId()).last("LIMIT 1");
        return Result.success(keeperService.toDTO(keeperMapper.selectOne(qw)));
    }

    /**
     * 【获取我的寄养员申请状态】
     *
     * API: GET /api/keepers/my-application
     *
     * 权限：需登录
     *
     * 场景：用户在申请入驻成为看护者后，查看自己的申请审核状态。
     * 与 /me 的区别：/me 仅对已审核通过的看护者返回数据，
     * 本接口对所有状态的申请都返回（PENDING / APPROVED / REJECTED）。
     */
    @GetMapping("/my-application")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取我的寄养员申请", description = "获取当前登录用户的寄养员申请信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<KeeperVO> getMyApplication(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("调用 getMyApplication()");
        return Result.success(keeperService.toDTO(keeperService.findByUserId(token.getUserId())));
    }

    /**
     * 【根据ID获取看护者详情】
     *
     * API: GET /api/keepers/{id}
     *
     * 权限：公开（无需登录）
     *
     * @param id 看护者 ID
     * @return 看护者详情（含关联商家信息）
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取看护者", description = "根据ID获取看护者详情")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<KeeperVO> getById(@Parameter(description = "看护者ID") @PathVariable Long id) {
        log.info("调用 getById()");
        return Result.success(keeperService.toDTO(keeperService.getById(id)));
    }

    /**
     * 【搜索附近看护者】
     *
     * API: GET /api/keepers/nearby?lat=...&lng=...&radius=...
     *
     * 权限：公开（无需登录）
     *
     * 场景：用户在首页或地图模式搜索附近的可接单看护者。
     * 基于 Haversine 公式计算距离，仅返回在线且已审核的看护者。
     */
    @GetMapping("/nearby")
    @Operation(summary = "搜索附近看护者", description = "根据经纬度和半径搜索附近看护者")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<KeeperVO>> searchNearby(@Parameter(description = "纬度") @RequestParam double lat, @Parameter(description = "经度") @RequestParam double lng, @Parameter(description = "搜索半径（公里）") @RequestParam double radius) {
        log.info("调用 searchNearby()");
        return Result.success(keeperService.searchNearby(lat, lng, radius));
    }

    /**
     * 【根据商户ID获取看护者列表】
     *
     * API: GET /api/keepers/merchant/{merchantId}
     *
     * 权限：公开（无需登录）
     *
     * 场景：用户在商家详情页查看该商家旗下的所有看护者。
     *
     * @param merchantId 商户 ID
     * @return 该商户旗下的看护者列表（仅已审核的）
     */
    @GetMapping("/merchant/{merchantId}")
    @Operation(summary = "根据商户获取看护者", description = "根据商户ID获取其下的所有看护者")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<KeeperVO>> findByMerchant(@Parameter(description = "商户ID") @PathVariable Long merchantId) {
        log.info("调用 findByMerchant()");
        return Result.success(keeperService.findByMerchantId(merchantId).stream().map(keeperService::toDTO).collect(Collectors.toList()));
    }

    /**
     * 【入驻申请成为看护者】
     *
     * API: POST /api/keepers
     *
     * 权限：需登录
     *
     * 场景：用户提交资质信息（身份证、宠物护理经验等），
     * 选择关联的商家（可选），申请成为看护者。
     *
     * 状态变化：创建后状态为 KEEPER_PENDING（待审核），
     * 需商户和管理员后续审核。
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "创建看护者", description = "当前用户注册为看护者")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<KeeperVO> create(@AuthenticationPrincipal JwtAuthenticationToken token, @Valid @RequestBody KeeperCreateRequestDTO dto) {
        log.info("调用 create()");
        return Result.success(keeperService.toDTO(keeperService.create(dto, token.getUserId())));
    }

    /**
     * 【更新看护者信息】
     *
     * API: PUT /api/keepers/{id}
     *
     * 权限：KEEPER / MERCHANT / ADMIN
     *
     * 校验：assertKeeperEditable — 仅看护者本人、所在商家管理员或平台管理员可操作。
     *
     * @param id  看护者 ID
     * @param dto 更新信息
     * @return 更新后的看护者
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('KEEPER','MERCHANT','ADMIN')")
    @Operation(summary = "更新看护者信息", description = "根据ID更新看护者信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<KeeperVO> update(@Parameter(description = "看护者ID") @PathVariable Long id, @Valid @RequestBody KeeperUpdateRequestDTO dto) {
        log.info("调用 update()");
        assertKeeperEditable(id);
        return Result.success(keeperService.toDTO(keeperService.update(id, dto)));
    }

    /**
     * 【删除看护者】
     *
     * API: DELETE /api/keepers/{id}
     *
     * 权限：仅 ADMIN
     *
     * 场景：管理员后台删除违规或离岗看护者。
     * 物理删除数据库记录（级联清理关联的请假、考勤等数据）。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "删除看护者", description = "根据ID删除看护者")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> delete(@Parameter(description = "看护者ID") @PathVariable Long id) {
        log.info("调用 delete()");
        keeperService.delete(id);
        return Result.success();
    }

    /**
     * 【看护者辞职】
     *
     * API: POST /api/keepers/{id}/resign
     *
     * 权限：仅 KEEPER
     *
     * 业务校验：
     * - 当前用户必须是该看护者本人
     * - 该看护者不能有未完成的订单（进行中或待处理）
     *
     * 状态变化：KEEPER_APPROVED → KEEPER_INACTIVE
     *
     * 触发联动：
     * - 看护者状态置为离线
     * - 关联的商家将不再展示该看护者
     */
    @PostMapping("/{id}/resign")
    @PreAuthorize("hasRole('KEEPER')")
    @Operation(summary = "看护者辞职", description = "看护者主动辞职，需完成未结束订单校验")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "辞职成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "404", description = "资源不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> resign(@Parameter(description = "看护者ID") @PathVariable Long id,
                               @AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("调用 resign()");
        keeperService.resign(id, token.getUserId());
        return Result.success();
    }

    /**
     * 【商家终止看护者关系】
     *
     * API: POST /api/keepers/{id}/merchant-terminate
     *
     * 权限：仅 MERCHANT
     *
     * 业务校验：当前商家必须是该看护者的归属商家，且无未完成订单。
     *
     * 触发联动：看护者在线状态置为离线，商家下不再展示该看护者。
     */
    @PostMapping("/{id}/merchant-terminate")
    @PreAuthorize("hasRole('MERCHANT')")
    @Operation(summary = "商家终止看护者关系", description = "商家终止与看护者的雇佣关系，需完成未结束订单校验")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "终止成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "404", description = "资源不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> merchantTerminate(@Parameter(description = "看护者ID") @PathVariable Long id,
                                          @AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("调用 merchantTerminate()");
        keeperService.terminateByMerchant(id, token.getUserId());
        return Result.success();
    }

    /**
     * 【管理员审核通过看护者】
     *
     * API: POST /api/keepers/{id}/approve
     *
     * 权限：仅 ADMIN
     *
     * 状态变化：KEEPER_PENDING → KEEPER_APPROVED
     *
     * 触发联动：看护者状态置为在线（ACTIVE），可开始接单。
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "审核通过看护者", description = "管理员审核通过看护者申请")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> approve(@Parameter(description = "看护者ID") @PathVariable Long id) {
        log.info("调用 approve()");
        keeperService.approve(id);
        return Result.success();
    }

    /**
     * 【管理员驳回看护者申请】
     *
     * API: POST /api/keepers/{id}/reject
     *
     * 权限：仅 ADMIN
     *
     * 状态变化：KEEPER_PENDING → KEEPER_REJECTED
     *
     * 触发联动：看护者状态置为离线（OFFLINE），需重新提交申请。
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "驳回看护者", description = "管理员驳回看护者申请")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> reject(@Parameter(description = "看护者ID") @PathVariable Long id) {
        log.info("调用 reject()");
        keeperService.reject(id);
        return Result.success();
    }

    /**
     * 【商户获取待审核寄养员列表】
     *
     * API: GET /api/keepers/merchant/pending
     *
     * 权限：仅 MERCHANT
     *
     * 场景：商户在后台查看归属本商户且状态为 KEEPER_PENDING 的寄养员，
     * 进行商户级审核（通过/驳回）。
     *
     * @param token 当前用户认证信息
     * @return 待审核寄养员列表
     */
    @GetMapping("/merchant/pending")
    @PreAuthorize("hasRole('MERCHANT')")
    @Operation(summary = "商户获取待审核寄养员", description = "商户获取归属本商户的待审核寄养员申请")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<KeeperVO>> listPendingByMerchant(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("调用 listPendingByMerchant()");
        return Result.success(
                keeperService.listPendingByMerchant(token.getUserId()).stream()
                        .map(keeperService::toDTO).collect(Collectors.toList()));
    }

    /**
     * 【商户审核通过寄养员】
     *
     * API: POST /api/keepers/{id}/merchant-approve
     *
     * 权限：仅 MERCHANT
     *
     * 业务校验：当前商户必须是该看护者的归属商家（merchant_id 匹配）。
     *
     * 状态变化：通过后仍需管理员最终审核（而非直接通过）。
     *
     * @param id 寄养员 ID
     * @param token 当前用户认证信息
     */
    @PostMapping("/{id}/merchant-approve")
    @PreAuthorize("hasRole('MERCHANT')")
    @Operation(summary = "商户审核通过寄养员", description = "商户审核通过归属本商户的寄养员申请")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> merchantApprove(@Parameter(description = "看护者ID") @PathVariable Long id,
                                        @AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("调用 merchantApprove()");
        keeperService.approveByMerchant(id, token.getUserId());
        return Result.success();
    }

    /**
     * 【商户驳回寄养员申请】
     *
     * API: POST /api/keepers/{id}/merchant-reject
     *
     * 权限：仅 MERCHANT
     *
     * 业务校验：当前商户必须是该看护者的归属商家。
     *
     * 状态变化：KEEPER_PENDING → KEEPER_REJECTED_BY_MERCHANT
     *
     * @param id 寄养员 ID
     * @param token 当前用户认证信息
     */
    @PostMapping("/{id}/merchant-reject")
    @PreAuthorize("hasRole('MERCHANT')")
    @Operation(summary = "商户驳回寄养员", description = "商户驳回归属本商户的寄养员申请")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> merchantReject(@Parameter(description = "看护者ID") @PathVariable Long id,
                                       @AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("调用 merchantReject()");
        keeperService.rejectByMerchant(id, token.getUserId());
        return Result.success();
    }

    /**
     * 【设置看护者在线状态】
     *
     * API: PATCH /api/keepers/{id}/online-status
     *
     * 权限：仅 ADMIN
     *
     * 场景：管理员在后台手动修正看护者的在线状态。
     *
     * 状态码映射：
     * - active / online / 1 → ACTIVE（在线可接单）
     * - offline / 3 → OFFLINE（离线）
     * - busy / 4 → BUSY（忙碌中，有进行中订单）
     *
     * @param id   看护者 ID
     * @param body 包含 status_wsh 字段的请求体
     */
    @PatchMapping("/{id}/online-status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "设置在线状态", description = "管理员手动修正看护者在线/离线/忙碌状态 (ACTIVE=1, OFFLINE=3, BUSY=4)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> setOnlineStatus(@Parameter(description = "看护者ID") @PathVariable Long id, @RequestBody KeeperOnlineStatusRequestDTO body) {
        log.info("调用 setOnlineStatus()");
        String statusStr = body.getStatus_wsh();
        int status;
        switch (statusStr != null ? statusStr.toLowerCase() : "") {
            case "active":
            case "online":
            case "1":
                status = 1;
                break;
            case "offline":
            case "3":
                status = 3;
                break;
            case "busy":
            case "4":
                status = 4;
                break;
            default:
                status = Integer.parseInt(statusStr);
        }
        keeperService.setOnlineStatus(id, status);
        return Result.success();
    }

    private void assertKeeperEditable(Long keeperId) {
        JwtAuthenticationToken token = currentToken();
        var keeper = keeperService.getById(keeperId);
        if (isAdmin(token)) {
            return;
        }
        if (token != null && token.getUserId().equals(keeper.getUser_id_wsh())) {
            return;
        }
        if (token != null && merchantService.isOwner(keeper.getMerchant_id_wsh(), token.getUserId())) {
            return;
        }
        throw new BusinessException(403, "No permission to update this keeper");
    }

    private JwtAuthenticationToken currentToken() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication instanceof JwtAuthenticationToken jwtToken ? jwtToken : null;
    }

    private boolean isAdmin(JwtAuthenticationToken token) {
        if (token == null) return false;
        for (GrantedAuthority authority : token.getAuthorities()) {
            if ("ROLE_ADMIN".equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
