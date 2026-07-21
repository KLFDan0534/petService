package com.pet.operation.controller;

import com.pet.common.BusinessException;
import com.pet.common.PageResult;
import com.pet.common.Result;
import com.pet.operation.dto.FavoriteCardDTO;
import com.pet.operation.dto.FavoriteDTO;
import com.pet.operation.dto.FavoriteTargetTypeDTO;
import com.pet.operation.dto.FavoriteToggleRequestDTO;
import com.pet.operation.entity.Favorite;
import com.pet.operation.service.FavoriteService;
import com.pet.security.JwtAuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/favorites")
@Tag(name = "【用户端】收藏管理", description = "用户收藏管理（收藏/取消收藏/查询）")
@Slf4j
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取收藏列表", description = "获取当前用户的收藏记录")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<FavoriteDTO>> list(@AuthenticationPrincipal JwtAuthenticationToken token,
                                          @RequestParam(value = "target_type_wsh", required = false) String target_type_wsh,
                                          @RequestParam(value = "targetType", required = false) String legacyTargetType) {
        log.info("list()");
        String targetType = firstNonBlank(target_type_wsh, legacyTargetType);
        List<Favorite> list = favoriteService.listByUser(token.getUserId(), targetType);
        return Result.success(list.stream().map(this::toDTO).collect(Collectors.toList()));
    }

    @GetMapping("/page")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "分页获取收藏列表", description = "获取分页的收藏卡片列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<PageResult<FavoriteCardDTO>> page(@AuthenticationPrincipal JwtAuthenticationToken token,
                                                    @RequestParam(value = "target_type_wsh", required = false) String target_type_wsh,
                                                    @RequestParam(value = "targetType", required = false) String legacyTargetType,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        log.info("page()");
        String targetType = firstNonBlank(target_type_wsh, legacyTargetType);
        return Result.success(favoriteService.pageByUser(token.getUserId(), targetType, page, size));
    }

    @GetMapping("/types")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取收藏类型列表", description = "返回支持的收藏目标类型")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<FavoriteTargetTypeDTO>> types() {
        log.info("types()");
        return Result.success(favoriteService.listTargetTypes());
    }

    @PostMapping("/toggle")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "切换收藏状态", description = "切换目标的收藏状态")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> toggle(@AuthenticationPrincipal JwtAuthenticationToken token,
                               @Valid @RequestBody FavoriteToggleRequestDTO body) {
        log.info("toggle()");
        favoriteService.toggle(token.getUserId(), body.getTarget_id_wsh(), body.getTarget_type_wsh());
        return Result.success();
    }

    @GetMapping("/check")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "检查收藏状态", description = "检查当前用户是否已收藏目标")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Boolean> check(@AuthenticationPrincipal JwtAuthenticationToken token,
                                 @RequestParam(value = "target_id_wsh", required = false) Long target_id_wsh,
                                 @RequestParam(value = "targetId", required = false) Long legacyTargetId,
                                 @RequestParam(value = "target_type_wsh", required = false) String target_type_wsh,
                                 @RequestParam(value = "targetType", required = false) String legacyTargetType) {
        log.info("check()");
        Long targetId = target_id_wsh != null ? target_id_wsh : legacyTargetId;
        String targetType = firstNonBlank(target_type_wsh, legacyTargetType);
        if (targetId == null) {
            throw new BusinessException(400, "target_id_wsh is required");
        }
        return Result.success(favoriteService.isFavorited(token.getUserId(), targetId, targetType));
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }

    private FavoriteDTO toDTO(Favorite entity) {
        FavoriteDTO dto = new FavoriteDTO();
        dto.setId_wsh(entity.getId_wsh());
        dto.setUser_id_wsh(entity.getUser_id_wsh());
        dto.setTarget_id_wsh(entity.getTarget_id_wsh());
        dto.setTarget_type_wsh(entity.getTarget_type_wsh());
        dto.setCreated_at_wsh(entity.getCreated_at_wsh());
        return dto;
    }
}
