package com.pet.boarding.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.boarding.entity.BusinessHours;
import com.pet.boarding.service.BusinessHoursService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 营业时间管理控制器
 * 提供商家营业时间的查询、设置和删除功能
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@RestController
@RequestMapping("/api/merchants/{merchantId}/hours")
@Tag(name = "营业时间管理", description = "商家营业时间的增删改查管理")
@Slf4j
public class BusinessHoursController {

    private final BusinessHoursService businessHoursService;

    public BusinessHoursController(BusinessHoursService businessHoursService) {
        this.businessHoursService = businessHoursService;
    }

    /**
     * 获取商家的营业时间列表
     * @param merchantId 商家ID
     * @return 营业时间列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping
    @Operation(summary = "获取营业时间", description = "获取商家的营业时间列表")
    public Result<List<BusinessHours>> list(@PathVariable Long merchantId) {
        log.info("list() called");
        return Result.success(businessHoursService.getByMerchantId(merchantId));
    }

    /**
     * 设置或更新某天的营业时间
     * @param merchantId 商家ID
     * @param hours 营业时间信息
     * @return 更新的营业时间
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping
    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN')")
    @Operation(summary = "设置营业时间", description = "设置或更新某天的营业时间")
    public Result<BusinessHours> upsert(@PathVariable Long merchantId, @RequestBody BusinessHours hours) {
        log.info("upsert() called");
        hours.setMerchant_id_wsh(merchantId);
        return Result.success(businessHoursService.upsert(hours));
    }

    /**
     * 删除某一天的营业时间记录
     * @param merchantId 商家ID
     * @param id 营业时间记录ID
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN')")
    @Operation(summary = "删除营业时间", description = "删除某一天的营业时间记录")
    public Result<Void> delete(@PathVariable Long merchantId, @PathVariable Long id) {
        log.info("delete() called");
        businessHoursService.delete(id);
        return Result.success();
    }
}
