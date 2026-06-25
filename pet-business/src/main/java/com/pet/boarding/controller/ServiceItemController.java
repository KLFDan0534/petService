package com.pet.boarding.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.boarding.entity.ServiceItem;
import com.pet.boarding.service.ServiceItemService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 服务项目管理控制器
 * 提供服务项目的列表、详情、新增、更新、删除及上下架管理功能
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@RestController
@RequestMapping("/api/services")
@Tag(name = "服务项目管理", description = "服务项目相关接口")
@Slf4j
public class ServiceItemController {

    private final ServiceItemService serviceItemService;

    public ServiceItemController(ServiceItemService serviceItemService) {
        this.serviceItemService = serviceItemService;
    }

    /**
     * 获取所有已上架的服务项目列表
     * @return 服务项目列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping
    @Operation(summary = "获取所有上架的服务项目列表", description = "获取所有已上架的服务项目")
    public Result<List<ServiceItem>> listAll() {
        log.info("listAll() called");
        return Result.success(serviceItemService.listAll());
    }

    /**
     * 根据商家ID获取该商家的所有服务项目
     * @param merchantId 商家ID
     * @return 服务项目列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/merchant/{merchantId}")
    @Operation(summary = "获取商家服务项目", description = "根据商家ID获取该商家的所有服务项目")
    public Result<List<ServiceItem>> listByMerchant(@PathVariable Long merchantId) {
        log.info("listByMerchant() called");
        return Result.success(serviceItemService.listByMerchant(merchantId));
    }

    /**
     * 根据ID获取服务项目详细信息
     * @param id 服务项目ID
     * @return 服务项目详情
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/{id}")
    @Operation(summary = "获取服务项目详情", description = "根据ID获取服务项目详细信息")
    public Result<ServiceItem> getById(@PathVariable Long id) {
        log.info("getById() called");
        return Result.success(serviceItemService.getById(id));
    }

    /**
     * 新增服务项目
     * @param item 服务项目信息
     * @return 创建的服务项目
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT')")
    @Operation(summary = "新增服务项目", description = "新增一个宠物服务项目")
    public Result<ServiceItem> create(@Valid @RequestBody ServiceItem item) {
        log.info("create() called");
        return Result.success(serviceItemService.create(item));
    }

    /**
     * 更新服务项目信息
     * @param id 服务项目ID
     * @param item 服务项目信息
     * @return 更新后的服务项目
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT')")
    @Operation(summary = "更新服务项目", description = "根据ID更新服务项目信息")
    public Result<ServiceItem> update(@PathVariable Long id, @Valid @RequestBody ServiceItem item) {
        log.info("update() called");
        item.setId_wsh(id);
        return Result.success(serviceItemService.update(item));
    }

    /**
     * 删除服务项目
     * @param id 服务项目ID
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT')")
    @Operation(summary = "删除服务项目", description = "根据ID删除服务项目")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("delete() called");
        serviceItemService.delete(id);
        return Result.success();
    }

    /**
     * 切换服务的上架/下架状态
     * @param id 服务项目ID
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/{id}/toggle-status")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT')")
    @Operation(summary = "上下架服务", description = "切换服务的上架/下架状态")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        log.info("toggleStatus() called");
        serviceItemService.toggleStatus(id);
        return Result.success();
    }

    /**
     * 更新服务图片URL列表
     * @param id 服务项目ID
     * @param body 请求体，包含images图片URL
     * @return 更新后的服务项目
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PutMapping("/{id}/images")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT')")
    @Operation(summary = "更新服务图片", description = "更新服务的图片URL列表")
    public Result<ServiceItem> updateImages(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        log.info("updateImages() called");
        return Result.success(serviceItemService.updateImages(id, body.get("images")));
    }
}
