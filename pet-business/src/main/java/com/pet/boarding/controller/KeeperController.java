package com.pet.boarding.controller;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.Result;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.security.JwtAuthenticationToken;
import com.pet.boarding.service.KeeperService;
import com.pet.boarding.vo.KeeperVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 看护者管理控制器
 * 提供看护者的列表、搜索、注册、审核及在线状态管理功能
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@RestController
@RequestMapping("/api/keepers")
@Tag(name = "看护者管理", description = "看护者信息管理，包括列表、搜索等")
@Slf4j
public class KeeperController {

    private final KeeperService keeperService;
    private final KeeperMapper keeperMapper;

    public KeeperController(KeeperService keeperService, KeeperMapper keeperMapper) {
        this.keeperService = keeperService;
        this.keeperMapper = keeperMapper;
    }

    /**
     * 获取所有已审核通过的看护者列表
     * @return 看护者列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping
    @Operation(summary = "获取所有已审核看护者", description = "获取平台上所有已审核通过的看护者列表")
    public Result<List<Keeper>> listAll() {
        log.info("调用 listAll()");
        return Result.success(keeperService.listAll());
    }

    /**
     * 管理员获取待审核看护者列表
     * @return 待审核看护者列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取待审核看护者", description = "管理员获取所有待审核的看护者申请")
    public Result<List<Keeper>> listPending() {
        log.info("调用 listPending()");
        return Result.success(keeperService.listPending());
    }

    /**
     * 获取当前登录用户的看护者信息
     * @param token 当前用户认证信息
     * @return 看护者信息
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取当前看护者信息", description = "根据当前登录用户获取看护者信息")
    public Result<Keeper> getCurrentKeeper(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("调用 getCurrentKeeper()");
        LambdaQueryWrapper<Keeper> qw = new LambdaQueryWrapper<>();
        qw.eq(Keeper::getUser_id_wsh, token.getUserId()).last("LIMIT 1");
        Keeper keeper = keeperMapper.selectOne(qw);
        return Result.success(keeper);
    }

    /**
     * 根据ID获取看护者详情
     * @param id 看护者ID
     * @return 看护者详情
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取看护者", description = "根据ID获取看护者详情")
    public Result<Keeper> getById(@PathVariable Long id) {
        log.info("调用 getById()");
        return Result.success(keeperService.getById(id));
    }

    /**
     * 搜索附近的看护者
     * @param lat 纬度
     * @param lng 经度
     * @param radius 搜索半径（公里）
     * @return 附近看护者列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/nearby")
    @Operation(summary = "搜索附近看护者", description = "根据经纬度和半径搜索附近看护者")
    public Result<List<KeeperVO>> searchNearby(@RequestParam double lat, @RequestParam double lng, @RequestParam double radius) {
        log.info("调用 searchNearby()");
        return Result.success(keeperService.searchNearby(lat, lng, radius));
    }

    /**
     * 根据商户ID获取其下的所有看护者
     * @param merchantId 商户ID
     * @return 看护者列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/merchant/{merchantId}")
    @Operation(summary = "根据商户获取看护者", description = "根据商户ID获取其下的所有看护者")
    public Result<List<Keeper>> findByMerchant(@PathVariable Long merchantId) {
        log.info("调用 findByMerchant()");
        return Result.success(keeperService.findByMerchantId(merchantId));
    }

    /**
     * 当前用户注册为看护者
     * @param token 当前用户认证信息
     * @param keeper 看护者信息
     * @return 创建的看护者
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "创建看护者", description = "当前用户注册为看护者")
    public Result<Keeper> create(@AuthenticationPrincipal JwtAuthenticationToken token, @Valid @RequestBody Keeper keeper) {
        log.info("调用 create()");
        keeper.setUser_id_wsh(token.getUserId());
        return Result.success(keeperService.create(keeper));
    }

    /**
     * 更新看护者信息
     * @param id 看护者ID
     * @param keeper 看护者信息
     * @return 更新后的看护者
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('KEEPER','MERCHANT','ADMIN')")
    @Operation(summary = "更新看护者信息", description = "根据ID更新看护者信息")
    public Result<Keeper> update(@PathVariable Long id, @Valid @RequestBody Keeper keeper) {
        log.info("调用 update()");
        return Result.success(keeperService.update(keeper));
    }

    /**
     * 删除看护者
     * @param id 看护者ID
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('KEEPER','MERCHANT','ADMIN')")
    @Operation(summary = "删除看护者", description = "根据ID删除看护者")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("调用 delete()");
        keeperService.delete(id);
        return Result.success();
    }

    /**
     * 管理员审核通过看护者申请
     * @param id 看护者ID
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "审核通过看护者", description = "管理员审核通过看护者申请")
    public Result<Void> approve(@PathVariable Long id) {
        log.info("调用 approve()");
        keeperService.approve(id);
        return Result.success();
    }

    /**
     * 管理员驳回看护者申请
     * @param id 看护者ID
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "驳回看护者", description = "管理员驳回看护者申请")
    public Result<Void> reject(@PathVariable Long id) {
        log.info("调用 reject()");
        keeperService.reject(id);
        return Result.success();
    }

    /**
     * 设置看护者在线状态（在线/离线/忙碌）
     * @param id 看护者ID
     * @param body 请求体，包含status_wsh状态值
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PatchMapping("/{id}/online-status")
    @PreAuthorize("hasAnyRole('KEEPER','ADMIN')")
    @Operation(summary = "设置在线状态", description = "看护者切换在线/离线/忙碌状态 (ACTIVE=1, OFFLINE=3, BUSY=4)")
    public Result<Void> setOnlineStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        log.info("调用 setOnlineStatus()");
        String statusStr = body.get("status_wsh");
        int status;
        switch (statusStr != null ? statusStr.toLowerCase() : "") {
            case "active":
            case "online":
            case "1":
                status = 1;
                break;
            case "offline":
            case "3":
                status = 3;
                break;
            case "busy":
            case "4":
                status = 4;
                break;
            default:
                status = Integer.parseInt(statusStr);
        }
        keeperService.setOnlineStatus(id, status);
        return Result.success();
    }
}
