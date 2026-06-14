package com.pet.module.service.controller;

import com.pet.common.Result;
import com.pet.module.service.entity.ServiceItem;
import com.pet.module.service.service.ServiceItemService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/services")
@Tag(name = "服务项目管理", description = "宠物服务项目的增删改查管理")
public class ServiceItemController {

    private final ServiceItemService serviceItemService;

    public ServiceItemController(ServiceItemService serviceItemService) {
        this.serviceItemService = serviceItemService;
    }

    @GetMapping("/merchant/{merchantId}")
    @Operation(summary = "获取商家服务项目", description = "根据商家ID获取该商家的所有服务项目")
    public Result<List<ServiceItem>> listByMerchant(@PathVariable Long merchantId) {
        return Result.success(serviceItemService.listByMerchant(merchantId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取服务项目详情", description = "根据ID获取服务项目详细信息")
    public Result<ServiceItem> getById(@PathVariable Long id) {
        return Result.success(serviceItemService.getById(id));
    }

    @PostMapping
    @Operation(summary = "新增服务项目", description = "新增一个宠物服务项目")
    public Result<ServiceItem> create(@RequestBody ServiceItem item) {
        return Result.success(serviceItemService.create(item));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新服务项目", description = "根据ID更新服务项目信息")
    public Result<ServiceItem> update(@PathVariable Long id, @RequestBody ServiceItem item) {
        item.setId(id);
        return Result.success(serviceItemService.update(item));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除服务项目", description = "根据ID删除服务项目")
    public Result<Void> delete(@PathVariable Long id) {
        serviceItemService.delete(id);
        return Result.success();
    }
}
