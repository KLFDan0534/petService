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

    /**
     * 【构造方法】注入资质服务
     *
     * 业务作用：由 Spring 注入 QualificationService，供资质查询与审核接口使用。
     *
     * 调用场景：Spring 容器启动创建 Controller Bean 时调用。
     *
     * 调用链：Spring 容器 → 本构造方法 → qualificationService 赋值。
     *
     * 数据处理：仅字段赋值。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：依赖缺失时由 Spring 抛出 Bean 创建异常。
     *
     * 注意事项：依赖为 final 字段。
     */
    public QualificationController(QualificationService qualificationService) {
        this.qualificationService = qualificationService;
    }

    /**
     * 【获取公开资质列表】按拥有者类型与ID返回已审核资质
     *
     * 业务作用：对外暴露 GET /api/qualifications/{ownerType}/{ownerId}，供商家/看护者详情页展示资质。
     *
     * 调用场景：用户查看商家或看护者资质时调用。
     *
     * 调用链：前端 → 本方法 → qualificationService.listByOwner（publicOnly=true）。
     *
     * 数据处理：ownerType 为路径变量（如 merchant/keeper），ownerId 为拥有者ID，返回 List<QualificationDTO>。
     *
     * 业务规则：只返回公开（已审核通过）的资质，未审核与被驳回的不展示。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：ownerType 非法时由服务层抛业务异常；数据库异常向上传播。
     *
     * 注意事项：本接口未加 @PreAuthorize，属匿名可访问的公开接口；ownerType 的合法取值由服务层决定，本层未做白名单校验。
     */
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

    /**
     * 【获取待审核资质】返回所有待审核的资质申请
     *
     * 业务作用：对外暴露 GET /api/qualifications/pending。
     *
     * 调用场景：后台资质审核列表页加载时调用。
     *
     * 调用链：前端 → 本方法 → qualificationService.listPending。
     *
     * 数据处理：无入参，返回 List<QualificationDTO>。
     *
     * 业务规则：仅返回待审核状态的数据。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：数据库异常向上传播。
     *
     * 注意事项：本方法未加 @PreAuthorize，任何调用方（含未登录）都能拉到待审核列表，存在越权风险；接口无分页。
     */
    @GetMapping("/pending")
    @Operation(summary = "获取待审核资质", description = "获取所有待审核的资质申请列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回待审核列表"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<QualificationDTO>> listPending() {
        return Result.success(qualificationService.listPending());
    }

    /**
     * 【审核通过资质】把指定资质置为审核通过
     *
     * 业务作用：对外暴露 POST /api/qualifications/{id}/approve。
     *
     * 调用场景：后台审核人员确认资质材料真实有效时调用。
     *
     * 调用链：前端 → 本方法 → qualificationService.approve。
     *
     * 数据处理：id 为资质ID，reviewerId 为审核人ID（请求参数），返回更新后的 QualificationDTO。
     *
     * 业务规则：reviewerId 必填；状态流转合法性由服务层校验。
     *
     * 状态影响：服务层更新资质审核状态与审核人。
     *
     * 异常情况：缺 reviewerId 时 Spring 抛 400；资质不存在或状态非法由服务层抛业务异常。
     *
     * 注意事项：reviewerId 直接来自请求参数而非登录态，未加 @PreAuthorize，理论上可伪造审核人；应改为从 JWT 取当前用户。
     */
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

    /**
     * 【驳回资质申请】把指定资质置为驳回并记录备注
     *
     * 业务作用：对外暴露 POST /api/qualifications/{id}/reject。
     *
     * 调用场景：后台审核人员发现资质材料不合规时调用。
     *
     * 调用链：前端 → 本方法 → qualificationService.reject。
     *
     * 数据处理：id、reviewerId 必填，remark 可选（驳回原因），返回更新后的 QualificationDTO。
     *
     * 业务规则：reviewerId 必填；remark 为空时由服务层决定是否允许。
     *
     * 状态影响：服务层更新资质状态为驳回并写入备注与审核人。
     *
     * 异常情况：缺 reviewerId 时 Spring 抛 400；资质不存在或状态非法由服务层抛业务异常。
     *
     * 注意事项：与 approve 相同，reviewerId 取自请求参数而非登录态，且本接口无权限注解。
     */
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
