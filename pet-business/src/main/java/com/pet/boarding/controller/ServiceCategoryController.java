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

/**
 * 【服务分类管理控制器】
 *
 * 业务作用：
 * 管理平台统一的服务分类体系，用户端查询启用分类，
 * 管理员端维护全部分类（含停用状态）。
 *
 * 权限要求：
 * - 用户查询接口（list / getById）：公开
 * - 管理接口（listAllForAdmin / create / update / delete）：仅 ADMIN
 *
 * API 路由前缀：/api/service-categories
 */
@RestController
@RequestMapping("/api/service-categories")
@Tag(name = "【用户端】服务分类管理", description = "平台统一服务分类管理（用户浏览/管理员维护）")
public class ServiceCategoryController {

    private final ServiceCategoryService categoryService;

    public ServiceCategoryController(ServiceCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * 获取所有启用分类
     *
     * <p>API: GET /api/service-categories/list</p>
     * <p>请求来源：前端服务列表页、下单页，用户浏览可用的服务分类</p>
     * <p>权限要求：无需登录，公开接口</p>
     * <p>输入参数：无</p>
     * <p>返回数据：List&lt;ServiceCategoryDTO&gt; - 所有启用的服务分类列表（平铺结构）</p>
     * <p>异常情况：
     * <ul>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    /**
     * 【获取所有启用分类】
     *
     * API: GET /api/service-categories/list
     *
     * 权限：公开
     *
     * 场景：用户端展示所有已启用的服务分类（平铺列表，非树形）。
     * 仅返回 status = ENABLED 的分类。
     */
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

    /**
     * 管理员获取所有分类
     *
     * <p>API: GET /api/service-categories/admin/list</p>
     * <p>请求来源：后台管理服务分类维护页，管理员查看全部分类（含停用）</p>
     * <p>权限要求：ADMIN角色（@PreAuthorize("hasRole('ADMIN')")）</p>
     * <p>输入参数：无</p>
     * <p>返回数据：List&lt;ServiceCategoryDTO&gt; - 所有服务分类列表（含启用和停用）</p>
     * <p>异常情况：
     * <ul>
     *   <li>403 - 非管理员无权限</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    /**
     * 【管理员获取所有分类】
     *
     * API: GET /api/service-categories/admin/list
     *
     * 权限：仅 ADMIN
     *
     * 场景：管理员在后台查看所有分类（含已停用的），以便维护。
     */
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

    /**
     * 【获取分类详情】
     *
     * API: GET /api/service-categories/{id}
     *
     * 权限：公开
     *
     * @param id 分类 ID
     */
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

    /**
     * 【新增分类】
     *
     * API: POST /api/service-categories
     *
     * 权限：仅 ADMIN
     *
     * 场景：管理员新增服务分类（如"狗狗寄养"、"猫咪寄养"等）。
     * 创建后默认状态为 ENABLED。
     *
     * @param request 分类创建信息
     */
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

    /**
     * 【更新分类】
     *
     * API: PUT /api/service-categories/{id}
     *
     * 权限：仅 ADMIN
     *
     * 场景：管理员修改分类名称、排序、状态等。
     *
     * @param id      分类 ID
     * @param request 更新信息
     */
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

    /**
     * 【删除分类】
     *
     * API: DELETE /api/service-categories/{id}
     *
     * 权限：仅 ADMIN
     *
     * 业务校验：删除前检查是否有子分类或服务项目引用，
     * 有引用时不允许删除（由 Service 层处理）。
     *
     * @param id 分类 ID
     */
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
