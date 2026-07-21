package com.pet.boarding.controller;

import com.pet.boarding.dto.ServiceItemCreateRequestDTO;
import com.pet.boarding.dto.ServiceItemDTO;
import com.pet.boarding.dto.ServiceItemUpdateImagesRequestDTO;
import com.pet.boarding.dto.ServiceItemUpdateRequestDTO;
import com.pet.boarding.entity.ServiceItem;
import com.pet.boarding.service.MerchantService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/services")
@Tag(name = "【用户端】服务项目管理", description = "商家服务项目CRUD管理（用户浏览/商家管理）")
@Slf4j
public class ServiceItemController {

    private final ServiceItemService serviceItemService;
    private final MerchantService merchantService;

    public ServiceItemController(ServiceItemService serviceItemService, MerchantService merchantService) {
        this.serviceItemService = serviceItemService;
        this.merchantService = merchantService;
    }

    @GetMapping
    @Operation(summary = "获取启用的服务项目列表", description = "获取所有启用的服务项目列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<ServiceItemDTO>> listAll() {
        log.info("listAll() called");
        return Result.success(serviceItemService.listAll().stream().map(serviceItemService::toDTO).collect(Collectors.toList()));
    }

    @GetMapping("/merchant/{merchantId}")
    @Operation(summary = "获取商家服务项目列表", description = "获取商家启用的服务项目列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "404", description = "商家不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<ServiceItemDTO>> listByMerchant(@Parameter(description = "商家ID") @PathVariable Long merchantId) {
        log.info("listByMerchant() called");
        return Result.success(serviceItemService.listByMerchant(merchantId).stream().map(serviceItemService::toDTO).collect(Collectors.toList()));
    }

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

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "按分类获取服务项目列表", description = "根据分类获取启用的服务项目")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "404", description = "分类不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<ServiceItemDTO>> listByCategory(@Parameter(description = "分类ID") @PathVariable Long categoryId) {
        log.info("listByCategory() called");
        return Result.success(serviceItemService.listByCategory(categoryId).stream().map(serviceItemService::toDTO).collect(Collectors.toList()));
    }

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
