package com.pet.order.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.PageRequestDTO;
import com.pet.common.PageResult;
import com.pet.common.Result;
import com.pet.order.dto.TipDTO;
import com.pet.order.dto.TipCreateRequestDTO;
import com.pet.order.service.TipService;
import com.pet.security.JwtAuthenticationToken;
import com.pet.system.entity.User;
import com.pet.system.mapper.UserMapper;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 小费控制器
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@RestController
@RequestMapping("/api/tips")
@Slf4j
@Tag(name = "【用户端】小费管理", description = "订单小费管理（用户赠送/查看）")
public class TipController {

    private final TipService tipService;
    private final UserMapper userMapper;

    public TipController(TipService tipService, UserMapper userMapper) {
        this.tipService = tipService;
        this.userMapper = userMapper;
    }

    /**
     * 给小费
     *
     * <p>API: POST /api/tips</p>
     * <p>请求来源：前端订单完成页或评价页，用户对看护者的服务满意后可额外给予小费</p>
     * <p>权限要求：已登录用户（@PreAuthorize("isAuthenticated()")）</p>
     * <p>输入参数：@body TipCreateRequestDTO - 包含订单ID、小费金额、收款看护者/商家ID等</p>
     * <p>返回数据：无（Result.success()）</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 小费金额不合法或订单状态不支持打赏</li>
     *   <li>401 - 未登录</li>
     *   <li>403 - 无权限（非订单所属用户）</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "给小费", description = "为指定的订单添加小费")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "给小费成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> create(@AuthenticationPrincipal JwtAuthenticationToken token,
                               @Valid @RequestBody TipCreateRequestDTO request) {
        log.info("create() 被调用");
        tipService.create(token.getUserId(), request);
        return Result.success();
    }

    /**
     * 管理员分页查询打赏记录
     *
     * <p>API: GET /api/tips/admin-list</p>
     * <p>请求来源：前端管理员后台-打赏记录页</p>
     * <p>权限要求：管理员（@PreAuthorize("hasRole('ADMIN')")）</p>
     * <p>输入参数：@pageParam PageRequestDTO 分页参数</p>
     * <p>返回数据：Result&lt;PageResult&lt;TipDTO&gt;&gt; - 分页的打赏记录，并填充打赏人/收款人昵称或用户名</p>
     * <p>异常情况：
     * <ul>
     *   <li>401 - 未登录</li>
     *   <li>403 - 无权限（非管理员）</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @GetMapping("/admin-list")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员分页查询打赏记录", description = "管理员分页查询所有打赏记录，并填充打赏人/收款人昵称或用户名")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回分页打赏记录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<PageResult<TipDTO>> adminList(PageRequestDTO pageParam) {
        log.info("adminList() 被调用, page: {}, size: {}", pageParam.getPage(), pageParam.getSize());
        var page = tipService.pageAll(pageParam);
        var dtoList = page.getRecords()
                .stream()
                .map(tip -> fillUserName(tipService.toDTO(tip)))
                .collect(Collectors.toList());
        PageResult<TipDTO> result = new PageResult<>();
        result.setList(dtoList);
        result.copyPageInfo(page);
        return Result.success(result);
    }

    /**
     * 为打赏DTO填充打赏人/收款人昵称或用户名
     *
     * 业务作用：
     * 根据 from_user_id/to_user_id 查询对应用户，优先取 nickname_wsh，为空时回退 username_wsh。
     *
     * @param dto 打赏DTO（可为null）
     * @return 填充昵称/用户名后的打赏DTO
     */
    private TipDTO fillUserName(TipDTO dto) {
        if (dto == null) return null;
        dto.setFrom_user_name_wsh(resolveUserName(dto.getFrom_user_id_wsh()));
        dto.setTo_user_name_wsh(resolveUserName(dto.getTo_user_id_wsh()));
        return dto;
    }

    /**
     * 根据用户ID解析昵称或用户名
     *
     * @param userId 用户ID（可为null）
     * @return 用户昵称或用户名，查不到时返回null
     */
    private String resolveUserName(Long userId) {
        if (userId == null) return null;
        User user = userMapper.selectById(userId);
        if (user == null) return null;
        if (user.getNickname_wsh() != null && !user.getNickname_wsh().isBlank()) {
            return user.getNickname_wsh();
        }
        return user.getUsername_wsh();
    }

    /**
     * 根据订单获取小费
     *
     * <p>API: GET /api/tips/order/{orderId}</p>
     * <p>请求来源：前端订单详情页，查看该订单的小费记录</p>
     * <p>权限要求：已登录用户（@PreAuthorize("isAuthenticated()")）</p>
     * <p>输入参数：@path orderId - 订单ID</p>
     * <p>返回数据：List&lt;TipDTO&gt; - 该订单的小费记录列表</p>
     * <p>异常情况：
     * <ul>
     *   <li>401 - 未登录</li>
     *   <li>403 - 无权限</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @GetMapping("/order/{orderId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "根据订单获取小费", description = "根据订单ID获取该订单的小费列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回小费列表"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<TipDTO>> listByOrder(@Parameter(description = "订单ID") @PathVariable Long orderId) {
        log.info("listByOrder() 被调用");
        return Result.success(tipService.listByOrder(orderId).stream().map(tipService::toDTO).collect(Collectors.toList()));
    }

    /**
     * 获取我的小费列表
     *
     * <p>API: GET /api/tips/me</p>
     * <p>请求来源：前端个人中心-我的打赏页，用户查看自己送出的小费记录</p>
     * <p>权限要求：已登录用户（@PreAuthorize("isAuthenticated()")）</p>
     * <p>输入参数：无（从token中提取用户ID）</p>
     * <p>返回数据：List&lt;TipDTO&gt; - 当前用户送出的所有小费记录</p>
     * <p>异常情况：
     * <ul>
     *   <li>401 - 未登录</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取我的小费列表", description = "获取当前用户的所有小费记录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回小费列表"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<TipDTO>> listMyTips(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("listMyTips() 被调用");
        return Result.success(tipService.listMyTips(token.getUserId()).stream().map(tipService::toDTO).collect(Collectors.toList()));
    }
}
