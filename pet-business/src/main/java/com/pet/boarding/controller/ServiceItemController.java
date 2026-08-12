package com.pet.boarding.controller;

import com.pet.boarding.dto.ServiceItemCreateRequestDTO;
import com.pet.boarding.dto.ServiceItemDTO;
import com.pet.boarding.dto.ServiceItemQueryDTO;
import com.pet.boarding.dto.ServiceItemUpdateImagesRequestDTO;
import com.pet.boarding.dto.ServiceItemUpdateRequestDTO;
import com.pet.boarding.dto.ServiceQueryResultVO;
import com.pet.boarding.dto.ServiceAvailabilityVO;
import com.pet.boarding.entity.ServiceItem;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 【服务项目管理控制器】
 *
 * 业务作用：
 * 管理商家提供的宠物寄养服务项目，包括服务项的 CRUD、状态切换、
 * 图片更新、按分类/商家查询等。
 *
 * 权限要求：
 * - 公开接口（GET）：任意用户
 * - 管理接口（POST/PUT/DELETE）：ADMIN 或 MERCHANT
 *
 * API 路由前缀：/api/services
 */
@RestController
@RequestMapping("/api/services")
@Tag(name = "【用户端】服务项目管理", description = "商家服务项目CRUD管理（用户浏览/商家管理）")
@Slf4j
public class ServiceItemController {

    private final ServiceItemService serviceItemService;
    private final MerchantService merchantService;
    private final ServiceAvailabilityService serviceAvailabilityService;

    public ServiceItemController(ServiceItemService serviceItemService, MerchantService merchantService,
                                 ServiceAvailabilityService serviceAvailabilityService) {
        this.serviceItemService = serviceItemService;
        this.merchantService = merchantService;
        this.serviceAvailabilityService = serviceAvailabilityService;
    }

    /**
     * 【获取所有启用的服务项目列表】
     *
     * API: GET /api/services
     *
     * 权限：公开
     *
     * 场景：用户浏览平台提供的所有服务项目。
     * 仅返回 status = ENABLED 的服务项。
     */
    @GetMapping
    @Operation(summary = "获取启用的服务项目列表", description = "获取所有启用的服务项目列表（支持分类/关键字/排序/分页）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<ServiceItemDTO>> listAll(@RequestParam(required = false) Long categoryId,
                                                @RequestParam(required = false) Long merchantId,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String sort,
                                                @RequestParam(required = false) BigDecimal latitude,
                                                @RequestParam(required = false) BigDecimal longitude,
                                                @RequestParam(required = false) Integer page,
                                                @RequestParam(required = false) Integer size) {
        log.info("listAll() called");
        return Result.success(serviceItemService.listPublic(query(categoryId, merchantId, keyword, sort, latitude, longitude, page, size)));
    }

    /**
     * 【公开服务列表分页查询】
     *
     * API: GET /api/services/public
     *
     * 权限：公开
     *
     * 场景：用户端服务浏览分页查询，返回总数。
     */
    @GetMapping("/public")
    @Operation(summary = "获取启用的服务项目分页列表", description = "公开服务浏览：分类/关键字/排序/分页/距离")
    public Result<ServiceQueryResultVO> pagePublic(@RequestParam(required = false) Long categoryId,
                                                   @RequestParam(required = false) Long merchantId,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String sort,
                                                   @RequestParam(required = false) BigDecimal latitude,
                                                   @RequestParam(required = false) BigDecimal longitude,
                                                   @RequestParam(required = false) Integer page,
                                                   @RequestParam(required = false) Integer size) {
        log.info("pagePublic() called");
        return Result.success(serviceItemService.queryPublic(query(categoryId, merchantId, keyword, sort, latitude, longitude, page, size)));
    }

    private ServiceItemQueryDTO query(Long categoryId, Long merchantId, String keyword, String sort,
                                      BigDecimal latitude, BigDecimal longitude, Integer page, Integer size) {
        ServiceItemQueryDTO q = new ServiceItemQueryDTO();
        q.setCategory_id_wsh(categoryId);
        q.setMerchant_id_wsh(merchantId);
        q.setKeyword_wsh(keyword);
        q.setSort_wsh(sort);
        q.setLatitude_wsh(latitude);
        q.setLongitude_wsh(longitude);
        q.setPage_wsh(page);
        q.setSize_wsh(size);
        return q;
    }

    /**
     * 【获取商家服务项目列表】
     *
     * API: GET /api/services/merchant/{merchantId}
     *
     * 权限：公开
     *
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
        log.info("listByMerchant() called");
        ServiceItemQueryDTO q = new ServiceItemQueryDTO();
        q.setMerchant_id_wsh(merchantId);
        q.setPage_wsh(1);
        q.setSize_wsh(100);
        return Result.success(serviceItemService.listPublic(q));
    }

    /**
     * 【获取服务项目详情】
     *
     * API: GET /api/services/{id}
     *
     * 权限：公开
     *
     * @param id 服务项目 ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取服务项目详情", description = "根据ID获取服务项目详情")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "404", description = "服务项目不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ServiceItemDTO> getById(@Parameter(description = "服务项目ID") @PathVariable Long id) {
        log.info("getById() called");
        return Result.success(serviceItemService.toDTO(serviceItemService.getById(id)));
    }

    /**
     * 【查询服务动态可预约性】
     *
     * API: GET /api/services/{serviceId}/availability?from=2026-08-12&to=2026-08-13&keeperId=5
     *
     * 权限：公开
     *
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
        log.info("availability() called, serviceId={}, from={}, to={}, keeperId={}", serviceId, from, to, keeperId);
        return Result.success(serviceAvailabilityService.getAvailability(serviceId, from, to, keeperId));
    }

    /**
     * 【创建服务项目】
     *
     * API: POST /api/services
     *
     * 权限：ADMIN 或 MERCHANT
     *
     * 场景：商家在后台新增一项宠物寄养服务（如"标准寄养"、"VIP 寄养"）。
     * 创建后默认状态为 ENABLED。
     *
     * 校验：assertMerchantOwnerOrAdmin — 仅商家本人或平台管理员可操作。
     *
     * @param dto 服务项目创建信息
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT')")
    @Operation(summary = "创建服务项目", description = "创建新的服务项目")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ServiceItemDTO> create(@Valid @RequestBody ServiceItemCreateRequestDTO dto) {
        log.info("create() called");
        assertMerchantOwnerOrAdmin(dto.getMerchant_id_wsh());
        return Result.success(serviceItemService.toDTO(serviceItemService.create(dto)));
    }

    /**
     * 【更新服务项目】
     *
     * API: PUT /api/services/{id}
     *
     * 权限：ADMIN 或 MERCHANT
     *
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
        log.info("update() called");
        assertServiceOwnerOrAdmin(id);
        return Result.success(serviceItemService.toDTO(serviceItemService.update(id, dto)));
    }

    /**
     * 【删除服务项目】
     *
     * API: DELETE /api/services/{id}
     *
     * 权限：ADMIN 或 MERCHANT
     *
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
        log.info("delete() called");
        assertServiceOwnerOrAdmin(id);
        serviceItemService.delete(id);
        return Result.success();
    }

    /**
     * 【切换服务项目状态】
     *
     * API: POST /api/services/{id}/toggle-status
     *
     * 权限：ADMIN 或 MERCHANT
     *
     * 场景：商家临时下架某项服务（如寄养满员时），或重新上架。
     *
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
        log.info("toggleStatus() called");
        assertServiceOwnerOrAdmin(id);
        serviceItemService.toggleStatus(id);
        return Result.success();
    }

    /**
     * 【按分类获取服务项目列表】
     *
     * API: GET /api/services/category/{categoryId}
     *
     * 权限：公开
     *
     * 场景：用户在分类浏览页面，查看某分类下的所有服务项目。
     *
     * @param categoryId 分类 ID
     */
    @GetMapping("/category/{categoryId}")
    @Operation(summary = "按分类获取服务项目列表", description = "根据分类获取启用的服务项目")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "404", description = "分类不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<ServiceItemDTO>> listByCategory(@Parameter(description = "分类ID") @PathVariable Long categoryId) {
        log.info("listByCategory() called");
        ServiceItemQueryDTO q = new ServiceItemQueryDTO();
        q.setCategory_id_wsh(categoryId);
        q.setPage_wsh(1);
        q.setSize_wsh(100);
        return Result.success(serviceItemService.listPublic(q));
    }

    /**
     * 【获取商家服务项目管理列表】
     *
     * API: GET /api/services/merchant/{merchantId}/manage
     *
     * 权限：ADMIN 或 MERCHANT
     *
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
        log.info("listByMerchantForManage() called");
        assertMerchantOwnerOrAdmin(merchantId);
        return Result.success(serviceItemService.listByMerchantForManage(merchantId).stream().map(serviceItemService::toDTO).collect(Collectors.toList()));
    }

    /**
     * 【更新服务项目图片】
     *
     * API: PUT /api/services/{id}/images
     *
     * 权限：ADMIN 或 MERCHANT
     *
     * 场景：商家更新服务项目的展示图片（如封面图、详情图集）。
     *
     * @param id   服务项目 ID
     * @param body 包含 images_wsh 图片链接数组的请求体
     */
    @PutMapping("/{id}/images")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT')")
    @Operation(summary = "更新服务项目图片", description = "更新服务项目的图片链接")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "服务项目不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ServiceItemDTO> updateImages(@Parameter(description = "服务项目ID") @PathVariable Long id, @Valid @RequestBody ServiceItemUpdateImagesRequestDTO body) {
        log.info("updateImages() called");
        assertServiceOwnerOrAdmin(id);
        return Result.success(serviceItemService.toDTO(serviceItemService.updateImages(id, body.getImages_wsh())));
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
