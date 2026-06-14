package com.pet.module.keeper.controller;

import com.pet.common.Result;
import com.pet.module.keeper.dto.KeeperVO;
import com.pet.module.keeper.entity.Keeper;
import com.pet.module.keeper.service.KeeperService;
import com.pet.security.JwtAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/keepers")
public class KeeperController {

    private final KeeperService keeperService;

    public KeeperController(KeeperService keeperService) {
        this.keeperService = keeperService;
    }

    @GetMapping
    public Result<List<Keeper>> listAll() {
        return Result.success(keeperService.listAll());
    }

    @GetMapping("/{id}")
    public Result<Keeper> getById(@PathVariable Long id) {
        return Result.success(keeperService.getById(id));
    }

    @GetMapping("/nearby")
    public Result<List<KeeperVO>> searchNearby(@RequestParam double lat,
                                                @RequestParam double lng,
                                                @RequestParam(defaultValue = "5") double radius) {
        return Result.success(keeperService.searchNearby(lat, lng, radius));
    }

    @GetMapping("/merchant/{merchantId}")
    public Result<List<Keeper>> findByMerchant(@PathVariable Long merchantId) {
        return Result.success(keeperService.findByMerchantId(merchantId));
    }

    @PostMapping
    public Result<Keeper> create(@AuthenticationPrincipal JwtAuthenticationToken token,
                                  @RequestBody Keeper keeper) {
        keeper.setUserId(token.getUserId());
        return Result.success(keeperService.create(keeper));
    }

    @PutMapping("/{id}")
    public Result<Keeper> update(@PathVariable Long id, @RequestBody Keeper keeper) {
        keeper.setId(id);
        return Result.success(keeperService.update(keeper));
    }
}
