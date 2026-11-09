package com.pet.operation.controller;

import com.pet.common.BusinessException;
import com.pet.common.PageRequestDTO;
import com.pet.common.PageResult;
import com.pet.common.Result;
import com.pet.operation.dto.FavoriteCardDTO;
import com.pet.operation.dto.FavoriteDTO;
import com.pet.operation.dto.FavoriteTargetTypeDTO;
import com.pet.operation.dto.FavoriteToggleRequestDTO;
import com.pet.operation.entity.Favorite;
import com.pet.operation.service.FavoriteService;
import com.pet.security.JwtAuthenticationToken;
import com.pet.system.entity.User;
import com.pet.system.mapper.UserMapper;
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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/favorites")
@Tag(name = "【用户端】收藏管理", description = "用户收藏管理（收藏/取消收藏/查询）")
@Slf4j
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final UserMapper userMapper;

    /**
     * 【构造函数：注入收藏服务与用户 Mapper】
     *
     * 业务作用：注入 FavoriteService 与 UserMapper，后者用于管理员列表补充用户名。
     *
     * 调用场景：容器启动创建 FavoriteController Bean 时自动调用。
     *
     * 调用链：Spring IoC → FavoriteController(构造) → favoriteService/userMapper 字段赋值。
     *
     * 数据处理：仅做字段赋值。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：控制器直接依赖 UserMapper 仅为填充展示字段。
     */
    public FavoriteController(FavoriteService favoriteService, UserMapper userMapper) {
        this.favoriteService = favoriteService;
        this.userMapper = userMapper;
    }

    /**
     * 【获取收藏列表】
     *
     * 业务作用：返回当前用户的收藏记录，可按目标类型筛选。
     *
     * 调用场景：用户端「我的收藏」列表页。
     *
     * 调用链：GET /api/favorites → FavoriteController.list → firstNonBlank → FavoriteService.listByUser → toDTO。
     *
     * 数据处理：目标类型优先取 target_type_wsh，为空时回退旧参数 targetType；查询结果逐条转 DTO。
     *
     * 业务规则：需登录；targetType 为空表示不筛选。
     *
     * 状态影响：只读。
     *
     * 异常情况：目标类型不受支持时由服务层抛 400。
     *
     * 注意事项：同时兼容新旧两套参数名，属于历史兼容逻辑。
     */
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
        log.info("调用 list()");
        String targetType = firstNonBlank(target_type_wsh, legacyTargetType);
        List<Favorite> list = favoriteService.listByUser(token.getUserId(), targetType);
        return Result.success(list.stream().map(this::toDTO).collect(Collectors.toList()));
    }

    /**
     * 【分页获取收藏卡片列表】
     *
     * 业务作用：分页返回收藏卡片，卡片含目标摘要信息。
     *
     * 调用场景：用户端收藏页滚动加载。
     *
     * 调用链：GET /api/favorites/page → FavoriteController.page → firstNonBlank → FavoriteService.pageByUser → FavoriteTargetResolver。
     *
     * 数据处理：目标类型做新旧参数兼容后，连同 page/size 下传，由服务层组装卡片。
     *
     * 业务规则：需登录；page 默认 1、size 默认 10，实际边界由服务层收敛。
     *
     * 状态影响：只读。
     *
     * 异常情况：目标类型不受支持时由服务层抛 400。
     *
     * 注意事项：与 list 接口的区别在于返回卡片摘要而非原始收藏记录。
     */
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
        log.info("调用 page()");
        String targetType = firstNonBlank(target_type_wsh, legacyTargetType);
        return Result.success(favoriteService.pageByUser(token.getUserId(), targetType, page, size));
    }

    /**
     * 【获取收藏类型列表】
     *
     * 业务作用：返回系统支持的收藏目标类型及其展示名称。
     *
     * 调用场景：收藏页的筛选标签渲染。
     *
     * 调用链：GET /api/favorites/types → FavoriteController.types → FavoriteService.listTargetTypes → FavoriteTargetType。
     *
     * 数据处理：服务层枚举类型码并组装 code/label 列表。
     *
     * 业务规则：需登录。
     *
     * 状态影响：只读。
     *
     * 异常情况：无。
     *
     * 注意事项：类型来源是 FavoriteTargetType 常量，新增收藏对象时需同步维护。
     */
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
        log.info("调用 types()");
        return Result.success(favoriteService.listTargetTypes());
    }

    /**
     * 【切换收藏状态】
     *
     * 业务作用：已收藏则取消收藏，未收藏则新增收藏。
     *
     * 调用场景：商品、商家、服务详情页的收藏按钮。
     *
     * 调用链：POST /api/favorites/toggle → FavoriteController.toggle → FavoriteService.toggle → FavoriteMapper。
     *
     * 数据处理：取请求体的 target_id_wsh 与 target_type_wsh 下传，接口本身无返回值。
     *
     * 业务规则：需登录；请求体经 @Valid 校验；类型必须受支持。
     *
     * 状态影响：新增或软删除一条 favorite 记录。
     *
     * 异常情况：类型不受支持抛 400；参数校验失败返回 400。
     *
     * 注意事项：服务层对软删除记录做了恢复处理以规避唯一索引冲突，前端无需感知。
     */
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
        log.info("调用 toggle()");
        favoriteService.toggle(token.getUserId(), body.getTarget_id_wsh(), body.getTarget_type_wsh());
        return Result.success();
    }

    /**
     * 【检查是否已收藏】
     *
     * 业务作用：判断当前用户是否收藏了指定目标。
     *
     * 调用场景：详情页初始化收藏按钮状态。
     *
     * 调用链：GET /api/favorites/check → FavoriteController.check → firstNonBlank → FavoriteService.isFavorited。
     *
     * 数据处理：目标 ID 优先取 target_id_wsh，为空回退 targetId；目标类型同样做新旧参数兼容。
     *
     * 业务规则：需登录；目标 ID 必填，缺失直接抛 400。
     *
     * 状态影响：只读。
     *
     * 异常情况：targetId 为空抛 BusinessException(400, "target_id_wsh is required")；类型不受支持由服务层抛 400。
     *
     * 注意事项：目标类型允许为空，此时服务层会做归一化处理。
     */
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
        log.info("调用 check()");
        Long targetId = target_id_wsh != null ? target_id_wsh : legacyTargetId;
        String targetType = firstNonBlank(target_type_wsh, legacyTargetType);
        if (targetId == null) {
            throw new BusinessException(400, "target_id_wsh is required");
        }
        return Result.success(favoriteService.isFavorited(token.getUserId(), targetId, targetType));
    }

    /**
     * 管理员分页查看全部收藏数据
     * @param pageParam 分页参数
     * @param target_type_wsh 目标类型，精确匹配（可选）
     * @return 分页收藏DTO列表，含用户昵称/用户名
     * @author: wsh
     * @date: 2026/8/22
     **/
    @GetMapping("/admin-list")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员收藏列表", description = "管理员分页查看全部收藏数据，支持按目标类型筛选")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<PageResult<FavoriteDTO>> adminList(PageRequestDTO pageParam,
            @Parameter(description = "目标类型，精确匹配") @RequestParam(value = "target_type_wsh", required = false) String target_type_wsh) {
        log.info("调用 adminList()");
        var page = favoriteService.pageAll(pageParam, target_type_wsh);
        List<FavoriteDTO> dtoList = page.getRecords().stream().map(this::toDTO).collect(Collectors.toList());
        // 批量补用户昵称，避免逐条查库
        fillUserName(dtoList);
        PageResult<FavoriteDTO> result = new PageResult<>();
        result.setList(dtoList);
        result.copyPageInfo(page);
        return Result.success(result);
    }

    /**
     * 批量填充用户昵称/用户名
     */
    private void fillUserName(List<FavoriteDTO> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            return;
        }
        List<Long> userIds = dtoList.stream()
                .map(FavoriteDTO::getUser_id_wsh)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return;
        }
        // 一次批量查用户，昵称优先用户名兜底
        Map<Long, String> nameMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(
                        User::getId_wsh,
                        u -> (u.getNickname_wsh() != null && !u.getNickname_wsh().isBlank())
                                ? u.getNickname_wsh() : u.getUsername_wsh(),
                        (a, b) -> a));
        for (FavoriteDTO dto : dtoList) {
            if (dto.getUser_id_wsh() != null) {
                dto.setUser_name_wsh(nameMap.get(dto.getUser_id_wsh()));
            }
        }
    }

    /**
     * 【取第一个非空字符串】
     *
     * 业务作用：优先返回 primary，为空时返回 fallback。
     *
     * 调用场景：list、page、check 兼容新旧参数名。
     *
     * 调用链：FavoriteController.list / page / check → firstNonBlank。
     *
     * 数据处理：primary 非 null 且非空白即返回 primary，否则返回 fallback。
     *
     * 业务规则：空白字符串视为未提供。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：两个参数都为空时返回 null，由调用方或服务层决定默认行为。
     */
    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }

    /**
     * 【收藏实体转 DTO】
     *
     * 业务作用：把 Favorite 实体字段拷贝到 FavoriteDTO。
     *
     * 调用场景：list、adminList 组装返回数据。
     *
     * 调用链：FavoriteController.list / adminList → toDTO。
     *
     * 数据处理：逐字段赋值 id、用户 ID、目标 ID、目标类型与创建时间。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：entity 为 null 时抛 NullPointerException。
     *
     * 注意事项：未使用 BeanUtils，字段需手工同步。
     */
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
