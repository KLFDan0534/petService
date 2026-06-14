package com.pet.module.service.controller;

import com.pet.common.Result;
import com.pet.module.service.entity.ServiceItem;
import com.pet.module.service.service.ServiceItemService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServiceItemController {

    private final ServiceItemService serviceItemService;

    public ServiceItemController(ServiceItemService serviceItemService) {
        this.serviceItemService = serviceItemService;
    }

    @GetMapping("/merchant/{merchantId}")
    public Result<List<ServiceItem>> listByMerchant(@PathVariable Long merchantId) {
        return Result.success(serviceItemService.listByMerchant(merchantId));
    }

    @GetMapping("/{id}")
    public Result<ServiceItem> getById(@PathVariable Long id) {
        return Result.success(serviceItemService.getById(id));
    }

    @PostMapping
    public Result<ServiceItem> create(@RequestBody ServiceItem item) {
        return Result.success(serviceItemService.create(item));
    }

    @PutMapping("/{id}")
    public Result<ServiceItem> update(@PathVariable Long id, @RequestBody ServiceItem item) {
        item.setId(id);
        return Result.success(serviceItemService.update(item));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        serviceItemService.delete(id);
        return Result.success();
    }
}
