package com.pet.boarding.controller;

import com.pet.common.Result;
import com.pet.boarding.dto.ServiceCategoryCreateRequestDTO;
import com.pet.boarding.dto.ServiceCategoryUpdateRequestDTO;
import com.pet.boarding.dto.ServiceCategoryDTO;
import com.pet.boarding.service.ServiceCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/service-categories")
@Tag(name = "【用户端】服务分类管理", description = "平台统一服务分类管理（用户浏览/管理员维护）")
public class ServiceCategoryController {

    private final ServiceCategoryService categoryService;

    public ServiceCategoryController(ServiceCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/list")
    @Operation(summary = "获取所有启用分类", description = "获取所有启用的服务分类列表（平铺）")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<ServiceCategoryDTO>> listAll() {
        return Result.success(categoryService.listAllEnabled().stream().map(categoryService::toDTO).collect(Collectors.toList()));
    }

    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员获取所有分类", description = "管理员获取启用和停用的服务分类列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<ServiceCategoryDTO>> listAllForAdmin() {
        return Result.success(categoryService.listAll().stream().map(categoryService::toDTO).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取分类详情", description = "根据ID获取服务分类详情")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ServiceCategoryDTO> getById(@Parameter(description = "分类ID") @PathVariable Long id) {
        return Result.success(categoryService.toDTO(categoryService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "新增分类", description = "管理员新增服务分类")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ServiceCategoryDTO> create(@Valid @RequestBody ServiceCategoryCreateRequestDTO request) {
        return Result.success(categoryService.toDTO(categoryService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新分类", description = "管理员更新服务分类")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ServiceCategoryDTO> update(@Parameter(description = "分类ID") @PathVariable Long id, @Valid @RequestBody ServiceCategoryUpdateRequestDTO request) {
        return Result.success(categoryService.toDTO(categoryService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "删除分类", description = "管理员删除服务分类（检查子分类和引用）")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> delete(@Parameter(description = "分类ID") @PathVariable Long id) {
        categoryService.delete(id);
        return Result.success();
    }
}
