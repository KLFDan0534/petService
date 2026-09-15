package com.pet.boarding.controller;

import com.pet.boarding.dto.ServiceItemCreateRequestDTO;
import com.pet.boarding.dto.ServiceItemDTO;
import com.pet.boarding.dto.ServiceItemQueryDTO;
import com.pet.boarding.dto.ServiceItemUpdateRequestDTO;
import com.pet.boarding.dto.ServiceManageDetailVO;
import com.pet.boarding.dto.ServiceProductDetailVO;
import com.pet.boarding.dto.ServiceQueryResultVO;
import com.pet.boarding.dto.ServiceAvailabilityVO;
import com.pet.boarding.entity.ServiceItem;
import com.pet.boarding.service.MerchantScopeResolver;
import com.pet.boarding.service.MerchantService;
import com.pet.boarding.service.ServiceAvailabilityService;
import com.pet.boarding.service.ServiceItemService;
import com.pet.common.BusinessException;
import com.pet.common.Result;
import com.pet.security.JwtAuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 【服务项目管理控制器】
 * 业务作用：
 * 管理商家提供的宠物寄养服务项目，包括服务项的 CRUD、状态切换、
 * 图片更新、按分类/商家查询等。
 * 权限要求：
 * - 公开接口（GET）：任意用户
 * - 管理接口（POST/PUT/DELETE）：ADMIN 或 MERCHANT
 * * API 路由前缀：/api/services
 */
@RestController
@RequestMapping("/api/services")
@Tag(name = "【用户端】服务项目管理", description = "商家服务项目CRUD管理（用户浏览/商家管理）")
@Slf4j
public class ServiceItemController {

    private final ServiceItemService serviceItemService;
    private final MerchantService merchantService;
    private final ServiceAvailabilityService serviceAvailabilityService;
    private final MerchantScopeResolver merchantScopeResolver;

    public ServiceItemController(ServiceItemService serviceItemService, MerchantService merchantService,
                                 ServiceAvailabilityService serviceAvailabilityService,
                                 MerchantScopeResolver merchantScopeResolver) {
        this.serviceItemService = serviceItemService;
        this.merchantService = merchantService;
        this.serviceAvailabilityService = serviceAvailabilityService;
        this.merchantScopeResolver = merchantScopeResolver;
    }

    /**
     * 【公开服务列表分页查询】

     * API: GET /api/services/public

     * 权限：公开

     * 场景：用户端服务浏览分页查询，返回总数。
     * 安卓端预留
     */
    @GetMapping("/public")
    @Operation(summary = "获取启用的服务项目分页列表", description = "公开服务浏览：分类/关键字/排序/分页/距离")
    public Result<ServiceQueryResultVO> pagePublic(ServiceItemQueryDTO queryDTO) {
        log.info("pagePublic() 被调用");
        return Result.success(serviceItemService.queryPublic(queryDTO));
    }

    /**
     * 【获取商家服务项目列表】

     * API: GET /api/services/merchant/{merchantId}

     * 权限：公开

     * 场景：用户在商家详情页查看该商家提供的所有服务项目。
     *
     * @param merchantId 商家 ID
     */
    @GetMapping("/merchant/{merchantId}")
    @Operation(summary = "获取商家服务项目列表", description = "获取商家启用的服务项目列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "404", description = "商家不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<ServiceItemDTO>> listByMerchant(@Parameter(description = "商家ID") @PathVariable Long merchantId) {
        log.info("listByMerchant() 被调用");
        ServiceItemQueryDTO q = new ServiceItemQueryDTO();
        q.setMerchant_id_wsh(merchantId);
        q.setPage_wsh(1);
        q.setSize_wsh(100);
        return Result.success(serviceItemService.queryPublic(q).getItems_wsh());
    }

    /**
     * 【获取服务项目详情（公共可见性受控）】

     * API: GET /api/services/{id}

     * 权限：公开

     * 场景：兼容旧客户端的服务详情读取。只返回通过公共可见性不变量
     * （上架服务 + 已审核商家 + 启用分类）的服务，图册字段只含可信URL。
     * 详情页权威数据请使用 GET /api/services/{serviceId}/detail。
     *
     * @param id 服务项目 ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取服务项目详情", description = "根据ID获取服务项目详情（公共可见性受控，图册可信化）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "404", description = "服务项目不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ServiceItemDTO> getById(@Parameter(description = "服务项目ID") @PathVariable Long id) {
        log.info("getById() 被调用");
        return Result.success(serviceItemService.getByIdPublic(id));
    }

    /**
     * 【获取服务产品公开详情投影】

     * API: GET /api/services/{serviceId}/detail

     * 权限：公开

     * 场景：服务详情页的权威数据源。返回白名单字段 + 有序可信图册 +
     * 评分聚合 + 服务版本 + 可预约性标记；禁用/未审核商家/禁用分类一律拒绝。
     *
     * @param serviceId 服务产品ID
     */
    @GetMapping("/{serviceId}/detail")
    @Operation(summary = "获取服务产品公开详情投影", description = "白名单字段 + 可信图册 + 评分 + 版本 + 可预约性")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "服务下架/商家未审核/分类下架/参数不合法"),
            @ApiResponse(responseCode = "404", description = "服务产品不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ServiceProductDetailVO> detail(@Parameter(description = "服务产品ID") @PathVariable Long serviceId) {
        log.info("detail() 被调用, serviceId={}", serviceId);
        return Result.success(serviceItemService.getPublicDetail(serviceId));
    }

    /**
     * 【获取服务产品管理详情】

     * API: GET /api/services/{serviceId}/manage

     * 权限：ADMIN 或 MERCHANT（归属商家本人）

     * 场景：管理端编辑单个服务时回显全部标量字段与图册条目。
     *
     * @param serviceId 服务产品ID
     */
    @GetMapping("/{serviceId}/manage")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT')")
    @Operation(summary = "获取服务产品管理详情", description = "标量字段 + 有序图册（含文件ID与封面标记）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "服务产品不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ServiceManageDetailVO> manageDetail(@Parameter(description = "服务产品ID") @PathVariable Long serviceId) {
        log.info("manageDetail() 被调用, serviceId={}", serviceId);
        assertServiceOwnerOrAdmin(serviceId);
        return Result.success(serviceItemService.getManageDetail(serviceId));
    }

    /**
     * 【查询服务动态可预约性】

     * API: GET /api/services/{serviceId}/availability?from=2026-08-12&to=2026-08-13&keeperId=5

     * 权限：公开

     * 场景：用户在服务详情页选择日期/看护员后，查询每天的可预约窗口与起始槽位。
     * 只读接口，不产生任何写操作。
     *
     * @param serviceId 服务ID
     * @param from      起始日期（包含，不早于今天）
     * @param to        结束日期（包含，跨度不超过 31 天）
     * @param keeperId  可选看护员ID
     */
    @GetMapping("/{serviceId}/availability")
    @Operation(summary = "查询服务动态可预约性", description = "按日返回营业窗口与起始槽位（可选叠加看护员维度）")
    public Result<ServiceAvailabilityVO> availability(
            @Parameter(description = "服务ID") @PathVariable Long serviceId,
            @Parameter(description = "起始日期 yyyy-MM-dd") @RequestParam LocalDate from,
            @Parameter(description = "结束日期 yyyy-MM-dd") @RequestParam LocalDate to,
            @Parameter(description = "可选看护员ID") @RequestParam(required = false) Long keeperId) {
        log.info("availability() 被调用, serviceId={}, from={}, to={}, keeperId={}", serviceId, from, to, keeperId);
        return Result.success(serviceAvailabilityService.getAvailability(serviceId, from, to, keeperId));
    }

    /**
     * 【创建服务项目（聚合）】

     * API: POST /api/services

     * 权限：ADMIN 或 MERCHANT

     * 场景：商家在后台新增一项宠物寄养服务（如"标准寄养"、"VIP 寄养"）。
     * 创建后默认状态为 ENABLED，标量与图册在同一事务内写入。

     * 归属：MERCHANT 自动派生自己所属商家；ADMIN 必须显式指定目标商家
     * merchantId（请求参数）。客户端提交的归属不参与选择。
     *
     * @param dto        服务项目创建信息（含图册）
     * @param merchantId 管理员显式指定的目标商家ID（MERCHANT 可省略）
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT')")
    @Operation(summary = "创建服务项目", description = "标量 + 图册聚合创建，归属商家由服务端派生")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "目标商家不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ServiceItemDTO> create(
            @Parameter(description = "ADMIN显式指定的目标商家ID（MERCHANT可省略）")
            @RequestParam(required = false) Long merchantId,
            @Valid @RequestBody ServiceItemCreateRequestDTO dto) {
        log.info("create() 被调用");
        Long derivedMerchantId = merchantScopeResolver.resolve(merchantId, currentToken());
        return Result.success(serviceItemService.toDTO(serviceItemService.create(derivedMerchantId, dto)));
    }

    /**
     * 【更新服务项目】

     * API: PUT /api/services/{id}

     * 权限：ADMIN 或 MERCHANT

     * 校验：assertServiceOwnerOrAdmin — 仅服务项所属商家或平台管理员可操作。
     *
     * @param id  服务项目 ID
     * @param dto 更新信息
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT')")
    @Operation(summary = "更新服务项目", description = "根据ID更新服务项目")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "服务项目不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ServiceItemDTO> update(@Parameter(description = "服务项目ID") @PathVariable Long id, @Valid @RequestBody ServiceItemUpdateRequestDTO dto) {
        log.info("update() 被调用");
        assertServiceOwnerOrAdmin(id);
        return Result.success(serviceItemService.toDTO(serviceItemService.update(id, dto)));
    }

    /**
     * 【删除服务项目】

     * API: DELETE /api/services/{id}

     * 限：ADMIN 或 MERCHANT

     * 业务校验：物理删除，需确认该服务项未被任何订单引用。
     * 若有引用则不允许删除（由 Service 层处理）。
     *
     * @param id 服务项目 ID
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT')")
    @Operation(summary = "删除服务项目", description = "根据ID删除服务项目")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "服务项目不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> delete(@Parameter(description = "服务项目ID") @PathVariable Long id) {
        log.info("delete() 被调用");
        assertServiceOwnerOrAdmin(id);
        serviceItemService.delete(id);
        return Result.success();
    }

    /**
     * 【切换服务项目状态】

     * API: POST /api/services/{id}/toggle-status

     * 权限：ADMIN 或 MERCHANT

     * 场景：商家临时下架某项服务（如寄养满员时），或重新上架。

     * 状态变化：ENABLED ↔ DISABLED 来回切换。
     *
     * @param id 服务项目 ID
     */
    @PostMapping("/{id}/toggle-status")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT')")
    @Operation(summary = "切换服务项目状态", description = "启用或禁用服务项目")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "服务项目不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> toggleStatus(@Parameter(description = "服务项目ID") @PathVariable Long id) {
        log.info("toggleStatus() 被调用");
        assertServiceOwnerOrAdmin(id);
        serviceItemService.toggleStatus(id);
        return Result.success();
    }

    /**
     * 【获取商家服务项目管理列表】

     * API:GET /api/services/merchant/{merchantId}/manage

     * 权限：ADMIN 或 MERCHANT

     * 场景：商家在管理后台查看自己所有的服务项目（含已禁用的），
     * 与 listByMerchant 的区别在于包含 DISABLED 状态的项目。
     *
     * @param merchantId 商家 ID
     */
    @GetMapping("/merchant/{merchantId}/manage")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT')")
    @Operation(summary = "获取商家服务项目管理列表", description = "获取商家管理的所有服务项目")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "商家不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<ServiceItemDTO>> listByMerchantForManage(@Parameter(description = "商家ID") @PathVariable Long merchantId) {
        log.info("listByMerchantForManage() 被调用");
        assertMerchantOwnerOrAdmin(merchantId);
        return Result.success(serviceItemService.listByMerchantForManage(merchantId).stream().map(serviceItemService::toDTO).collect(Collectors.toList()));
    }

    private void assertServiceOwnerOrAdmin(Long serviceId) {
        ServiceItem item = serviceItemService.getById(serviceId);
        assertMerchantOwnerOrAdmin(item.getMerchant_id_wsh());
    }

    private void assertMerchantOwnerOrAdmin(Long merchantId) {
        JwtAuthenticationToken token = currentToken();
        if (isAdmin(token)) {
            return;
        }
        if (token == null || !merchantService.isOwner(merchantId, token.getUserId())) {
            throw new BusinessException(403, "No permission to manage this merchant service");
        }
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
