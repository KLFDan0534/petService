package com.pet.boarding.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.boarding.entity.Address;
import com.pet.security.JwtAuthenticationToken;
import com.pet.boarding.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 地址管理控制器
 * 提供当前用户地址的增删改查及默认地址设置功能
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@RestController
@RequestMapping("/api/addresses")
@Tag(name = "地址管理", description = "用户地址增删改查")
@Slf4j
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    /**
     * 获取当前用户的地址列表
     * @param token 当前用户认证信息
     * @return 地址列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取地址列表", description = "获取当前用户的地址列表")
    public Result<List<Address>> list(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("list() called");
        return Result.success(addressService.listByUser(token.getUserId()));
    }

    /**
     * 根据ID获取地址详情
     * @param token 当前用户认证信息
     * @param id 地址ID
     * @return 地址详情
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取地址详情", description = "根据ID获取地址详情")
    public Result<Address> get(@AuthenticationPrincipal JwtAuthenticationToken token,
                               @PathVariable Long id) {
        log.info("get() called");
        Address address = addressService.getById(id);
        if (address == null) {
            return Result.error(404, "�1�7�1�7�0�7�1�7�1�7�1�7�1�7�1�7�1�7");
        }
        return Result.success(addressService.getById(id));
    }

    /**
     * 新增地址
     * @param token 当前用户认证信息
     * @param addr 地址信息
     * @return 创建的地址
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "新增地址", description = "新增用户地址")
    public Result<Address> create(@AuthenticationPrincipal JwtAuthenticationToken token,
                                  @Valid @RequestBody Address addr) {
        log.info("create() called");
        return Result.success(addressService.create(token.getUserId(), addr));
    }

    /**
     * 更新地址信息
     * @param token 当前用户认证信息
     * @param id 地址ID
     * @param addr 地址信息
     * @return 更新后的地址
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "更新地址", description = "更新指定地址信息")
    public Result<Address> update(@AuthenticationPrincipal JwtAuthenticationToken token,
                                  @PathVariable Long id,
                                  @Valid @RequestBody Address addr) {
        log.info("update() called");
        return Result.success(addressService.update(token.getUserId(), id, addr));
    }

    /**
     * 删除地址
     * @param token 当前用户认证信息
     * @param id 地址ID
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "删除地址", description = "删除指定地址")
    public Result<Void> delete(@AuthenticationPrincipal JwtAuthenticationToken token,
                               @PathVariable Long id) {
        log.info("delete()被调用");
        addressService.delete(token.getUserId(), id);
        return Result.success();
    }

    /**
     * 设置默认地址
     * @param token 当前用户认证信息
     * @param id 地址ID
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/{id}/default")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "设置默认地址", description = "将指定地址设为默认地址")
    public Result<Void> setDefault(@AuthenticationPrincipal JwtAuthenticationToken token,
                                   @PathVariable Long id) {
        log.info("setDefault()被调用");
        addressService.setDefault(token.getUserId(), id);
        return Result.success();
    }
}
