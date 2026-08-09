package com.pet.boarding.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.boarding.dto.AddressCreateRequestDTO;
import com.pet.boarding.dto.AddressDTO;
import com.pet.boarding.dto.AddressUpdateRequestDTO;
import com.pet.security.JwtAuthenticationToken;
import com.pet.boarding.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 【地址管理控制器】
 *
 * 业务作用：
 * 管理当前登录用户的收货/联系地址，包括地址的 CRUD
 * 以及默认地址设置功能。
 *
 * 权限要求：
 * 所有接口需登录（isAuthenticated），仅操作本人的地址。
 *
 * API 路由前缀：/api/addresses
 */
@RestController
@RequestMapping("/api/addresses")
@Tag(name = "【用户端】地址管理", description = "用户地址增删改查及默认地址设置")
@Slf4j
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    /**
     * 【获取当前用户的地址列表】
     *
     * API: GET /api/addresses
     *
     * 权限：需登录
     *
     * 场景：用户在个人中心管理地址簿。
     * 返回当前用户的所有地址，默认地址排在首位。
     *
     * @param token 当前用户认证信息
     * @return 地址列表（默认地址排首位）
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取地址列表", description = "获取当前用户的地址列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<AddressDTO>> list(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("list() called");
        return Result.success(addressService.listByUser(token.getUserId()).stream().map(addressService::toDTO).collect(Collectors.toList()));
    }

    /**
     * 【获取地址详情】
     *
     * API: GET /api/addresses/{id}
     *
     * 权限：需登录
     *
     * @param id 地址 ID
     * @return 地址详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取地址详情", description = "根据ID获取地址详情")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<AddressDTO> get(@AuthenticationPrincipal JwtAuthenticationToken token,
                               @Parameter(description = "地址ID") @PathVariable Long id) {
        log.info("get() called");
        return Result.success(addressService.toDTO(addressService.getById(id)));
    }

    /**
     * 【新增地址】
     *
     * API: POST /api/addresses
     *
     * 权限：需登录
     *
     * 场景：用户在地址簿中添加新的收货地址。
     * 如果当前用户无地址，则第一个地址自动设为默认地址。
     *
     * @param dto 地址创建信息
     * @return 创建的地址
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "新增地址", description = "新增用户地址")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<AddressDTO> create(@AuthenticationPrincipal JwtAuthenticationToken token,
                                  @Valid @RequestBody AddressCreateRequestDTO dto) {
        log.info("create() called");
        return Result.success(addressService.toDTO(addressService.create(token.getUserId(), dto)));
    }

    /**
     * 【更新地址信息】
     *
     * API: PUT /api/addresses/{id}
     *
     * 权限：需登录
     *
     * 业务校验：仅更新当前用户本人的地址。
     *
     * @param id  地址 ID
     * @param dto 更新信息
     * @return 更新后的地址
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "更新地址", description = "更新指定地址信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<AddressDTO> update(@AuthenticationPrincipal JwtAuthenticationToken token,
                                  @Parameter(description = "地址ID") @PathVariable Long id,
                                  @Valid @RequestBody AddressUpdateRequestDTO dto) {
        log.info("update() called");
        return Result.success(addressService.toDTO(addressService.update(token.getUserId(), id, dto)));
    }

    /**
     * 【删除地址】
     *
     * API: DELETE /api/addresses/{id}
     *
     * 权限：需登录
     *
     * 业务校验：仅删除当前用户本人的地址。
     * 若删除的是默认地址，则自动将剩余地址中最新的一条设为默认。
     *
     * @param id 地址 ID
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "删除地址", description = "删除指定地址")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> delete(@AuthenticationPrincipal JwtAuthenticationToken token,
                               @Parameter(description = "地址ID") @PathVariable Long id) {
        log.info("delete()被调用");
        addressService.delete(token.getUserId(), id);
        return Result.success();
    }

    /**
     * 【设置默认地址】
     *
     * API: POST /api/addresses/{id}/default
     *
     * 权限：需登录
     *
     * 业务规则：同一用户只有一个默认地址。
     * 设置新默认地址时，自动取消原默认地址的标记。
     *
     * @param id 地址 ID
     */
    @PostMapping("/{id}/default")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "设置默认地址", description = "将指定地址设为默认地址")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> setDefault(@AuthenticationPrincipal JwtAuthenticationToken token,
                                   @Parameter(description = "地址ID") @PathVariable Long id) {
        log.info("setDefault()被调用");
        addressService.setDefault(token.getUserId(), id);
        return Result.success();
    }
}
