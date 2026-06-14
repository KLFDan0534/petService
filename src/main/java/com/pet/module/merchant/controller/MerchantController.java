package com.pet.module.merchant.controller;

import com.pet.common.Result;
import com.pet.module.merchant.entity.Merchant;
import com.pet.module.merchant.service.MerchantService;
import com.pet.security.JwtAuthenticationToken;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/merchants")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @GetMapping
    public Result<List<Merchant>> listAll() {
        return Result.success(merchantService.listAll());
    }

    @GetMapping("/{id}")
    public Result<Merchant> getById(@PathVariable Long id) {
        return Result.success(merchantService.getById(id));
    }

    @GetMapping("/nearby")
    public Result<List<Merchant>> searchNearby(@RequestParam double lat,
                                                @RequestParam double lng,
                                                @RequestParam(defaultValue = "5") double radius) {
        return Result.success(merchantService.searchNearby(lat, lng, radius));
    }

    @PostMapping
    public Result<Merchant> create(@AuthenticationPrincipal JwtAuthenticationToken token,
                                    @RequestBody Merchant merchant) {
        merchant.setUserId(token.getUserId());
        return Result.success(merchantService.create(merchant));
    }

    @PutMapping("/{id}")
    public Result<Merchant> update(@PathVariable Long id, @RequestBody Merchant merchant) {
        merchant.setId(id);
        return Result.success(merchantService.update(merchant));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> approve(@PathVariable Long id) {
        merchantService.approve(id);
        return Result.success();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> reject(@PathVariable Long id) {
        merchantService.reject(id);
        return Result.success();
    }
}
