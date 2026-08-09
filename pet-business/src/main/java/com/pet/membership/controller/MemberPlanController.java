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

    /**
     * 获取启用会员套餐
     *
     * <p>API: GET /api/membership/plans/active</p>
     * <p>请求来源：前端会员中心-购买会员页，用户查看当前可购买的会员套餐</p>
     * <p>权限要求：已登录用户（@PreAuthorize("isAuthenticated()")）</p>
     * <p>输入参数：无</p>
     * <p>返回数据：List&lt;MemberPlanDTO&gt; - 所有启用状态的会员套餐列表（含价格、权益等）</p>
     * <p>异常情况：
     * <ul>
     *   <li>401 - 未登录</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
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

    /**
     * 管理员获取会员套餐
     *
     * <p>API: GET /api/membership/admin/plans</p>
     * <p>请求来源：后台管理会员套餐配置页，管理员查看启用和停用的套餐</p>
     * <p>权限要求：ADMIN角色（@PreAuthorize("hasRole('ADMIN')")）</p>
     * <p>输入参数：@query status_wsh - 套餐状态筛选（可选）</p>
     * <p>返回数据：List&lt;MemberPlanDTO&gt; - 所有或指定状态的会员套餐列表</p>
     * <p>异常情况：
     * <ul>
     *   <li>403 - 非管理员无权限</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
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

    /**
     * 管理员获取会员套餐详情
     *
     * <p>API: GET /api/membership/admin/plans/{id}</p>
     * <p>请求来源：后台管理会员套餐编辑页，管理员查看套餐详情</p>
     * <p>权限要求：ADMIN角色（@PreAuthorize("hasRole('ADMIN')")）</p>
     * <p>输入参数：@path id - 套餐ID</p>
     * <p>返回数据：MemberPlanDTO - 套餐详情</p>
     * <p>异常情况：
     * <ul>
     *   <li>403 - 非管理员无权限</li>
     *   <li>404 - 套餐不存在</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
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

    /**
     * 新增会员套餐
     *
     * <p>API: POST /api/membership/admin/plans</p>
     * <p>请求来源：后台管理会员套餐配置页，管理员新增会员套餐</p>
     * <p>权限要求：ADMIN角色（@PreAuthorize("hasRole('ADMIN')")）</p>
     * <p>输入参数：@body MemberPlanCreateRequestDTO - 包含套餐名称、价格、时长、权益描述等</p>
     * <p>返回数据：MemberPlanDTO - 新创建的套餐信息</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 参数校验失败</li>
     *   <li>403 - 非管理员无权限</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
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

    /**
     * 更新会员套餐
     *
     * <p>API: PUT /api/membership/admin/plans/{id}</p>
     * <p>请求来源：后台管理会员套餐编辑页，管理员更新套餐信息</p>
     * <p>权限要求：ADMIN角色（@PreAuthorize("hasRole('ADMIN')")）</p>
     * <p>输入参数：
     * <ul>
     *   <li>@path id - 套餐ID</li>
     *   <li>@body MemberPlanUpdateRequestDTO - 包含更新的套餐字段</li>
     * </ul>
     * </p>
     * <p>返回数据：MemberPlanDTO - 更新后的套餐信息</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 参数错误或套餐不存在</li>
     *   <li>403 - 非管理员无权限</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
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

    /**
     * 更新会员套餐状态
     *
     * <p>API: POST /api/membership/admin/plans/{id}/status</p>
     * <p>请求来源：后台管理会员套餐配置页，管理员启用或停用套餐</p>
     * <p>权限要求：ADMIN角色（@PreAuthorize("hasRole('ADMIN')")）</p>
     * <p>输入参数：
     * <ul>
     *   <li>@path id - 套餐ID</li>
     *   <li>@body MemberPlanStatusRequestDTO - 包含目标状态status_wsh</li>
     * </ul>
     * </p>
     * <p>返回数据：MemberPlanDTO - 更新状态后的套餐信息</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 无效的状态值</li>
     *   <li>403 - 非管理员无权限</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
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

    /**
     * 删除会员套餐
     *
     * <p>API: DELETE /api/membership/admin/plans/{id}</p>
     * <p>请求来源：后台管理会员套餐配置页，管理员删除套餐</p>
     * <p>权限要求：ADMIN角色（@PreAuthorize("hasRole('ADMIN')")）</p>
     * <p>输入参数：@path id - 套餐ID</p>
     * <p>返回数据：无（Result.success()），删除前会检查是否被会员或会员订单引用</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 套餐被引用无法删除</li>
     *   <li>403 - 非管理员无权限</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
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
