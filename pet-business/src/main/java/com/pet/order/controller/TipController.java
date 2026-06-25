package com.pet.order.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.order.entity.Tip;
import com.pet.order.service.TipService;
import com.pet.security.JwtAuthenticationToken;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 小费控制器
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@RestController
@RequestMapping("/api/tips")
@Slf4j
public class TipController {

    private final TipService tipService;

    public TipController(TipService tipService) {
        this.tipService = tipService;
    }

    /**
     * 给小费
     * @param token 当前用户认证信息
     * @param tip 小费信息
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public Result<Void> create(@AuthenticationPrincipal JwtAuthenticationToken token,
                               @Valid @RequestBody Tip tip) {
        log.info("create() called");
        tipService.create(token.getUserId(), tip);
        return Result.success();
    }

    /**
     * 根据订单ID获取小费列表
     * @param orderId 订单ID
     * @return 小费列表
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @GetMapping("/order/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public Result<List<Tip>> listByOrder(@PathVariable Long orderId) {
        log.info("listByOrder() called");
        return Result.success(tipService.listByOrder(orderId));
    }

    /**
     * 获取当前用户的小费列表
     * @param token 当前用户认证信息
     * @return 小费列表
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public Result<List<Tip>> listMyTips(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("listMyTips() called");
        return Result.success(tipService.listMyTips(token.getUserId()));
    }
}
