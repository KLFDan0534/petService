package com.pet.order.controller;

import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.service.MerchantService;
import com.pet.common.BusinessException;
import com.pet.common.OrderStatus;
import com.pet.common.PageResult;
import com.pet.common.Result;
import com.pet.common.annotation.LogOperation;
import com.pet.operation.entity.FileRecord;
import com.pet.operation.service.FileRecordService;
import com.pet.operation.service.impl.MinIoService;
import com.pet.order.dto.OrderAssignRequestDTO;
import com.pet.order.dto.OrderCheckoutDTO;
import com.pet.order.dto.OrderCompleteRequestDTO;
import com.pet.order.dto.OrderCreateRequestDTO;
import com.pet.order.dto.OrderCancelRequestDTO;
import com.pet.order.dto.OrderDTO;
import com.pet.order.dto.OrderPreviewRequestDTO;
import com.pet.order.dto.OrderProcessRequestDTO;
import com.pet.order.dto.OrderQuoteDTO;
import com.pet.order.dto.OrderReceivedRequestDTO;
import com.pet.order.dto.OrderScrollResult;
import com.pet.order.dto.OrderStartRequestDTO;
import com.pet.order.dto.OrderUpdateStatusRequestDTO;
import com.pet.order.entity.PetOrder;
import com.pet.order.service.OrderService;
import com.pet.security.JwtAuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 订单控制器
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "【用户端】订单管理", description = "宠物服务订单管理（用户/商家/看护者/管理员使用）")
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final KeeperMapper keeperMapper;
    private final MerchantService merchantService;
    private final MinIoService minIoService;
    private final FileRecordService fileRecordService;

    /**
     * 业务作用：注入订单服务、看护者 Mapper、商家服务、对象存储与文件记录服务。
     * 异常情况：无；依赖缺失时由 Spring 启动期报错。
     * 注意事项：直接注入 KeeperMapper 用于按用户反查看护者，未走 KeeperService。
     */
    public OrderController(OrderService orderService,
                           KeeperMapper keeperMapper,
                           MerchantService merchantService,
                           MinIoService minIoService,
                           FileRecordService fileRecordService) {
        this.orderService = orderService;
        this.keeperMapper = keeperMapper;
        this.merchantService = merchantService;
        this.minIoService = minIoService;
        this.fileRecordService = fileRecordService;
    }

    /**
     * 获取当前用户的订单列表
     * @param token 当前用户认证信息
     * @return 订单列表
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取当前用户的订单列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<OrderDTO>> listMyOrders(@AuthenticationPrincipal JwtAuthenticationToken token) {
        // 接口语义：获取「当前认证用户」的订单列表，与角色无关。
        // 管理员查看全平台订单应走专门的管理后台接口，不应复用「我的订单」端点。
        return Result.success(orderService.listByOwner(token.getUserId()));
    }

    /**
     * 获取商户订单列表
     *
     * @param token 当前用户认证信息
     * @param page 页码
     * @param size 每页大小
     * @return 商户订单列表及分页信息
     * @author: wsh
     * @date: 2026/06/24 11:05
     * @deprecated 这个端点是假分页：service 返回全量，total 直接取 list.size()，
     *             page/size 只是原样回显，翻页拿不到新数据。
     *             请改用 {@code GET /api/orders/merchant/scroll}（游标分页，恒定快）。
     *             保留仅为兼容旧客户端，前端已全部切换，下个版本删除。
     */
    @Deprecated
    @GetMapping("/merchant")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取商户订单列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<PageResult<OrderDTO>> listMerchantOrders(@AuthenticationPrincipal JwtAuthenticationToken token,
                                                           @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
                                                           @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size) {
        var merchant = merchantService.findByUserId(token.getUserId());
        if (merchant == null) {
            return Result.error(404, "商户不存在");
        }
        List<OrderDTO> list = orderService.listByMerchant(merchant.getId_wsh());
        PageResult<OrderDTO> result = new PageResult<>();
        result.setList(list);
        result.setTotal(list.size());
        result.setPage(page);
        result.setSize(size);
        return Result.success(result);
    }

    /**
     * 获取看护者订单列表
     * @param token 当前用户认证信息
     * @return 看护者订单列表
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @GetMapping("/my-keeper")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取看护者订单列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<OrderDTO>> listKeeperOrders(@AuthenticationPrincipal JwtAuthenticationToken token) {
        Long keeperId = findKeeperIdByUserId(token.getUserId());
        if (keeperId == null) {
            return Result.success(Collections.emptyList());
        }
        return Result.success(orderService.listByKeeper(keeperId));
    }

    // =========================================================================
    // 分页查询端点
    //
    // C 端四个走游标（滚动加载，恒定快），后台一个走页码（可跳页/对账）。
    // 老接口（/、/merchant、/my-keeper、/pool）保留不动，前端切换完成后再清理。
    // =========================================================================

    /**
     * 我的订单（游标分页）
     *
     * API: GET /api/orders/scroll
     * 场景：用户端「我的订单」滚动加载。size 只支持 20/50/100。
     */
    @GetMapping("/scroll")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "我的订单（游标分页）",
            description = "首页不传 cursor；下一页传上一页返回的 next_cursor_wsh；size 只支持 20/50/100")
    public Result<OrderScrollResult> scrollMyOrders(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Parameter(description = "游标，首页不传") @RequestParam(required = false) String cursor,
            @Parameter(description = "每页条数 20/50/100") @RequestParam(required = false, defaultValue = "20") Integer size,
            @Parameter(description = "状态过滤") @RequestParam(required = false) String status,
            @Parameter(description = "订单号模糊搜索") @RequestParam(required = false) String keyword) {
        return Result.success(orderService.scrollByOwner(token.getUserId(), cursor, size, status, keyword));
    }

    /**
     * 商家订单（游标分页）
     *
     * API: GET /api/orders/merchant/scroll
     * 场景：商家端订单管理滚动加载。替代原 /merchant 的假分页。
     */
    @GetMapping("/merchant/scroll")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "商家订单（游标分页）", description = "真分页，size 只支持 20/50/100")
    public Result<OrderScrollResult> scrollMerchantOrders(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Parameter(description = "游标，首页不传") @RequestParam(required = false) String cursor,
            @Parameter(description = "每页条数 20/50/100") @RequestParam(required = false, defaultValue = "20") Integer size,
            @Parameter(description = "状态过滤") @RequestParam(required = false) String status,
            @Parameter(description = "订单号模糊搜索") @RequestParam(required = false) String keyword) {
        var merchant = merchantService.findByUserId(token.getUserId());
        if (merchant == null) {
            return Result.error(404, "商户不存在");
        }
        return Result.success(orderService.scrollByMerchant(merchant.getId_wsh(), cursor, size, status, keyword));
    }

    /**
     * 看护者任务（游标分页）
     *
     * API: GET /api/orders/keeper/scroll
     * 场景：看护者端「我的任务」滚动加载。
     */
    @GetMapping("/keeper/scroll")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "看护者任务（游标分页）", description = "size 只支持 20/50/100")
    public Result<OrderScrollResult> scrollKeeperOrders(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Parameter(description = "游标，首页不传") @RequestParam(required = false) String cursor,
            @Parameter(description = "每页条数 20/50/100") @RequestParam(required = false, defaultValue = "20") Integer size,
            @Parameter(description = "状态过滤") @RequestParam(required = false) String status,
            @Parameter(description = "订单号模糊搜索") @RequestParam(required = false) String keyword) {
        Long keeperId = findKeeperIdByUserId(token.getUserId());
        if (keeperId == null) {
            return Result.success(OrderScrollResult.empty());
        }
        return Result.success(orderService.scrollByKeeper(keeperId, cursor, size, status, keyword));
    }

    /**
     * 我的订单各状态计数（订单页标签用）
     *
     * API: GET /api/orders/stats
     * 为什么单独一个接口：列表改成游标分页后，前端只持有已加载的那一页，
     * 标签上的数字如果在前端算就会随滚动变化，必须由服务端按全量聚合。
     */
    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "我的订单状态计数", description = "返回 status_wsh → 数量，用于标签数字")
    public Result<Map<String, Long>> myOrderStats(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(orderService.countByOwnerStatus(token.getUserId()));
    }

    /**
     * 商家订单各状态计数（商家订单页标签用）
     *
     * API: GET /api/orders/merchant/stats
     */
    @GetMapping("/merchant/stats")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "商家订单状态计数", description = "返回 status_wsh → 数量")
    public Result<Map<String, Long>> merchantOrderStats(@AuthenticationPrincipal JwtAuthenticationToken token) {
        var merchant = merchantService.findByUserId(token.getUserId());
        if (merchant == null) {
            return Result.error(404, "商户不存在");
        }
        return Result.success(orderService.countByMerchantStatus(merchant.getId_wsh()));
    }

    /**
     * 待分配订单池（游标分页）
     *
     * API: GET /api/orders/pool/scroll
     * 场景：看护者抢单页滚动加载，按创建时间升序（先到先得）。
     */
    @GetMapping("/pool/scroll")
    @PreAuthorize("hasRole('KEEPER')")
    @Operation(summary = "待分配订单池（游标分页）", description = "按创建时间升序，size 只支持 20/50/100")
    public Result<OrderScrollResult> scrollPool(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Parameter(description = "游标，首页不传") @RequestParam(required = false) String cursor,
            @Parameter(description = "每页条数 20/50/100") @RequestParam(required = false, defaultValue = "20") Integer size) {
        return Result.success(orderService.scrollAssignableForKeeper(token.getUserId(), cursor, size));
    }

    /**
     * 全平台订单（后台页码分页）
     *
     * API: GET /api/orders/admin/page
     * 场景：管理后台订单列表。不传日期时默认最近 30 天；page*size 上限 2000。
     */
    @GetMapping("/admin/page")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "全平台订单（后台分页）",
            description = "不传日期默认最近 30 天；翻页深度上限 2000 条；size 只支持 20/50/100")
    public Result<PageResult<OrderDTO>> pageAllOrders(
            @Parameter(description = "页码，从 1 开始") @RequestParam(required = false, defaultValue = "1") Integer page,
            @Parameter(description = "每页条数 20/50/100") @RequestParam(required = false, defaultValue = "20") Integer size,
            @Parameter(description = "状态过滤") @RequestParam(required = false) String status,
            @Parameter(description = "订单号模糊搜索") @RequestParam(required = false) String keyword,
            @Parameter(description = "创建日期起 yyyy-MM-dd") @RequestParam(required = false) String start_date,
            @Parameter(description = "创建日期止 yyyy-MM-dd") @RequestParam(required = false) String end_date) {
        return Result.success(orderService.pageForAdmin(page, size, status, keyword,
                parseDate(start_date), parseDate(end_date)));
    }

    /**
     * 业务作用：把 yyyy-MM-dd 字符串解析为 LocalDate。
     * 业务规则：空值返回 null，表示不限制。
     * 异常情况：格式非法抛 400，不静默忽略（否则用户以为筛选生效了）。
     */
    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            throw new BusinessException(400, "日期格式应为 yyyy-MM-dd");
        }
    }

    /**
     * 获取订单详情
     * @param token 当前用户认证信息
     * @param id 订单ID
     * @return 订单详情
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取订单详情")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<OrderDTO> getById(@AuthenticationPrincipal JwtAuthenticationToken token, @Parameter(description = "订单ID") @PathVariable Long id) {
        var order = orderService.getById(id);
        Long userId = token.getUserId();
        boolean owner = order.getOwner_id_wsh() != null && order.getOwner_id_wsh().equals(userId);
        boolean keeper = isKeeperParticipant(userId, order.getKeeper_id_wsh());
        boolean merchant = isMerchantParticipant(userId, order.getMerchant_id_wsh());
        if (!owner && !keeper && !merchant && !JwtAuthenticationToken.isAdmin(token)) {
            return Result.error(403, "无权访问此订单");
        }
        OrderDTO dto = orderService.toDTOEnriched(order);
        // toDTO 默认已把交接码/接走码置空，这里只对主人/管理员放行明文
        if (owner || JwtAuthenticationToken.isAdmin(token)) {
            dto.setHandover_code_wsh(order.getHandover_code_wsh());
            // 接走码只在「服务中且尚未确认接走」时给主人/管理员；keeper 即使参与方也读不到
            if (OrderStatus.IN_PROGRESS.equals(order.getStatus_wsh())
                    && order.getCheckout_confirmed_at_wsh() == null) {
                dto.setPickup_code_wsh(order.getPickup_code_wsh());
            }
        }
        return Result.success(dto);
    }

    /**
     * 创建订单
     * @param token 当前用户认证信息
     * @param request 创建订单请求体
     * @return 创建的订单
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @LogOperation(module = "Order", operation = "增加/创建", description = "创建订单")
    @Operation(summary = "创建订单")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<OrderDTO> create(@AuthenticationPrincipal JwtAuthenticationToken token,
                                   @Valid @RequestBody OrderCreateRequestDTO request) {
        return Result.success(orderService.createOrder(token.getUserId(), request));
    }

    /**
     * 价格预览（不创建订单）
     *
     * 与创建订单共用同一套计价与校验代码，返回的金额即为提交后的实付金额。
     *
     * @param token   当前用户认证信息
     * @param request 预览请求体
     * @return 金额明细与服务时间
     */
    @PostMapping("/preview")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "价格预览（不创建订单）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<OrderQuoteDTO> preview(@AuthenticationPrincipal JwtAuthenticationToken token,
                                         @Valid @RequestBody OrderPreviewRequestDTO request) {
        return Result.success(orderService.previewOrder(token.getUserId(), request));
    }

    /**
     * 取消订单
     * @param token 当前用户认证信息
     * @param body 请求体，包含orderId或orderNo
     * @return 无返回值
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @PostMapping("/cancel")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(module = "Order", operation = "取消", description = "取消订单")
    @Operation(summary = "取消订单")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> cancel(@AuthenticationPrincipal JwtAuthenticationToken token,
                               @RequestBody OrderCancelRequestDTO body) {
        body = requireBody(body);
        Long orderId = body.getOrder_id_wsh();
        if (orderId != null) {
            orderService.cancelOrderById(token.getUserId(), orderId);
        } else {
            orderService.cancelOrder(token.getUserId(), body.getOrder_no_wsh());
        }
        return Result.success();
    }

    /**
     * 【待分配订单池】
     *
     * API: GET /api/orders/pool
     * 权限：KEEPER
     * 场景：看护者查看本商家名下已支付、尚无负责人的订单，用于抢单。
     *
     * @param token 当前用户认证信息
     * @return 可抢订单列表
     */
    @GetMapping("/pool")
    @PreAuthorize("hasRole('KEEPER')")
    @Operation(summary = "待分配订单池", description = "看护者查看本商家名下待分配的已支付订单")
    public Result<List<OrderDTO>> pool(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(orderService.listAssignableForKeeper(token.getUserId()));
    }

    /**
     * 【看护者抢单】
     *
     * API: POST /api/orders/claim
     * 权限：KEEPER
     * 场景：看护者从待分配池领取订单，成功即进入 confirmed。
     *
     * @param token 当前用户认证信息
     * @param body  请求体，包含订单号
     * @return 无返回值
     */
    @PostMapping("/claim")
    @PreAuthorize("hasRole('KEEPER')")
    @LogOperation(module = "Order", operation = "Claim", description = "Claim order")
    @Operation(summary = "看护者抢单")
    public Result<Void> claim(@AuthenticationPrincipal JwtAuthenticationToken token,
                              @Valid @RequestBody OrderProcessRequestDTO body) {
        body = requireBody(body);
        orderService.claimOrder(token.getUserId(), body.getOrder_no_wsh());
        return Result.success();
    }

    /**
     * 【商家指派负责人】
     *
     * API: POST /api/orders/assign
     * 权限：MERCHANT
     * 场景：商家管理员把待分配订单指派给名下某个看护者。
     *
     * @param token 当前用户认证信息
     * @param body  请求体，包含订单号与目标看护者ID
     * @return 无返回值
     */
    @PostMapping("/assign")
    @PreAuthorize("hasRole('MERCHANT')")
    @LogOperation(module = "Order", operation = "Assign", description = "Assign order to keeper")
    @Operation(summary = "商家指派看护者")
    public Result<Void> assign(@AuthenticationPrincipal JwtAuthenticationToken token,
                               @Valid @RequestBody OrderAssignRequestDTO body) {
        orderService.assignOrder(token.getUserId(), body.getOrder_no_wsh(), body.getKeeper_id_wsh());
        return Result.success();
    }

    /**
     * 拒绝订单
     * @param token 当前用户认证信息
     * @param body 请求体，包含orderNo
     * @return 无返回值
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @PostMapping("/reject")
    @PreAuthorize("hasRole('KEEPER') or hasRole('MERCHANT')")
    @LogOperation(module = "Order", operation = "Reject", description = "Reject order")
    @Operation(summary = "拒绝订单")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> reject(@AuthenticationPrincipal JwtAuthenticationToken token,
                               @Valid @RequestBody OrderProcessRequestDTO body) {
        body = requireBody(body);
        orderService.rejectOrder(token.getUserId(), body.getOrder_no_wsh());
        return Result.success();
    }

    /**
     * 确认接收宠物
     * @param token 当前用户认证信息
     * @param body 请求体，包含orderNo和handoverCode
     * @return 无返回值
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @PostMapping("/received")
    @PreAuthorize("hasRole('KEEPER') or hasRole('MERCHANT')")
    @LogOperation(module = "Order", operation = "Received", description = "Confirm pet received")
    @Operation(summary = "确认接收宠物")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> markReceived(@AuthenticationPrincipal JwtAuthenticationToken token,
                                     @Valid @RequestBody OrderReceivedRequestDTO body) {
        body = requireBody(body);
        orderService.markReceived(token.getUserId(), body);
        return Result.success();
    }

    /**
     * 开始服务
     * @param token 当前用户认证信息
     * @param body 请求体，包含orderNo和startPhoto
     * @return 无返回值
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @PostMapping("/start")
    @PreAuthorize("hasRole('KEEPER') or hasRole('MERCHANT')")
    @LogOperation(module = "Order", operation = "Start", description = "Start service")
    @Operation(summary = "开始服务")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> startService(@AuthenticationPrincipal JwtAuthenticationToken token,
                                     @Valid @RequestBody OrderStartRequestDTO body) {
        body = requireBody(body);
        orderService.startService(token.getUserId(), body.getOrder_no_wsh(), body.getStart_photo_wsh());
        return Result.success();
    }

    /**
     * 上传开始照片并开始服务
     * @param token 当前用户认证信息
     * @param orderNoWsh 订单号
     * @param orderNo 订单号（备用字段名）
     * @param file 照片文件
     * @return 无返回值
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @PostMapping(value = "/start/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('KEEPER') or hasRole('MERCHANT')")
    @LogOperation(module = "Order", operation = "Start", description = "Upload start photo and start service")
    @Operation(summary = "上传照片并开始服务")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> startServiceWithPhoto(@AuthenticationPrincipal JwtAuthenticationToken token,
                                               @Parameter(description = "订单号") @RequestParam(value = "order_no_wsh", required = false) String orderNoWsh,
                                               @Parameter(description = "订单号（备用）") @RequestParam(value = "orderNo", required = false) String orderNo,
                                               @Parameter(description = "照片文件") @RequestParam("file") MultipartFile file) {
        String resolvedOrderNo = firstNonBlank(orderNoWsh, orderNo);
        if (resolvedOrderNo == null || resolvedOrderNo.isBlank()) {
            throw new BusinessException(400, "订单号不能为空");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "开始照片不能为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new BusinessException(400, "开始照片必须是图片");
        }

        // 教学注释：文件上传属于外部副作用，必须先做权限和状态校验。
        // 后面的 startService 仍会再校验一次，用来防止校验后订单状态被并发改掉。
        orderService.validateStartServiceAccess(token.getUserId(), resolvedOrderNo.trim());
        PetOrder order = orderService.getByOrderNo(resolvedOrderNo.trim());
        String objectName = minIoService.uploadFile(file, "orders/" + order.getId_wsh() + "/start");
        FileRecord record = new FileRecord();
        record.setOriginal_name_wsh(file.getOriginalFilename());
        record.setObject_name_wsh(objectName);
        record.setSize_wsh(file.getSize());
        record.setContent_type_wsh(file.getContentType());
        record.setUser_id_wsh(token.getUserId());
        fileRecordService.create(record);
        orderService.startService(token.getUserId(), resolvedOrderNo.trim(), minIoService.getFileUrl(objectName));
        return Result.success();
    }

    /**
     * 确认接走（完成订单）
     * <p>
     * keeper 输入独立接走码，服务端固定实际接走时刻并结算晚接费。
     * 无补费直接完成；有补费返回补费单号，等主人余额支付后自动完成。
     *
     * @param token 当前用户认证信息
     * @param body  订单号 + 独立接走码
     * @return 接走结果（冻结时间、晚接小时、补费金额、补费单号、是否已完成）
     */
    @PostMapping("/complete")
    @PreAuthorize("hasRole('KEEPER') or hasRole('MERCHANT')")
    @LogOperation(module = "Order", operation = "Complete", description = "Confirm pet checkout")
    @Operation(summary = "确认接走（完成订单）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<OrderCheckoutDTO> complete(@AuthenticationPrincipal JwtAuthenticationToken token,
                                             @Valid @RequestBody OrderCompleteRequestDTO body) {
        body = requireBody(body);
        return Result.success(orderService.completeOrder(token.getUserId(), body));
    }

    /**
     * 更新订单状态
     * @param id 订单ID
     * @param body 请求体，包含status
     * @return 无返回值
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新订单状态")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> updateStatus(@Parameter(description = "订单ID") @PathVariable Long id, @Valid @RequestBody OrderUpdateStatusRequestDTO body) {
        body = requireBody(body);
        PetOrder order = orderService.getById(id);
        orderService.updateOrderStatus(order.getOrder_no_wsh(), body.getStatus_wsh());
        return Result.success();
    }

    /**
     * 【删除订单】
     *
     * API: DELETE /api/orders/{id}
     *
     * 权限：仅 ADMIN
     *
     * 业务作用：
     * 管理员删除订单记录。采用 @TableLogic 逻辑删除（deleted_wsh=1），
     * 不物理删除，不影响支付/退款/履约等关联数据，也不触发资金操作。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "删除订单", description = "逻辑删除指定订单")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> delete(@Parameter(description = "订单ID") @PathVariable Long id) {
        log.info("调用 delete()");
        orderService.delete(id);
        return Result.success();
    }

    /**
     * 业务作用：统一校验请求体是否为空，为空时抛出参数错误。
     * 业务规则：请求体为 null 一律视为 400 参数错误。
     * 异常情况：body 为 null → BusinessException(400, 请求体不能为空)。
     * 注意事项：泛型返回便于写成 body = requireBody(body) 的链式赋值。
     */
    private <T> T requireBody(T body) {
        if (body == null) {
            throw new BusinessException(400, "请求体不能为空");
        }
        return body;
    }

    /**
     * 业务作用：在两个候选订单号之间取第一个非空白值。
     * 数据处理：first 非空白返回 first，否则返回 second。
     * 业务规则：空白字符串视为无效。
     * 注意事项：两者都为空时返回 null，由调用方抛 400。
     */
    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    /**
     * 业务作用：根据登录用户 ID 反查看护者记录，取出看护者主键。
     * 业务规则：一个用户最多对应一个看护者，用 LIMIT 1 兜底。
     * 异常情况：无；用户不是看护者时返回 null。
     * 注意事项：每次调用都会查库，同一请求内可能重复查询。
     */
    private Long findKeeperIdByUserId(Long userId) {
        Keeper keeper = keeperMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Keeper>()
                        .eq(Keeper::getUser_id_wsh, userId)
                        .last("LIMIT 1"));
        return keeper != null ? keeper.getId_wsh() : null;
    }

    /**
     * 业务作用：判断当前用户是否为该订单的接单看护者。
     * 数据处理：任一入参为 null 直接返回 false，否则比对看护者主键。
     * 业务规则：仅看护者本人算参与方，同商家其他看护者不算。
     * 注意事项：内部会触发一次数据库查询。
     */
    private boolean isKeeperParticipant(Long userId, Long keeperId) {
        if (userId == null || keeperId == null) {
            return false;
        }
        Long currentKeeperId = findKeeperIdByUserId(userId);
        return keeperId.equals(currentKeeperId);
    }

    /**
     * 业务作用：判断当前用户是否为该订单的归属商家。
     * 数据处理：任一入参为 null 返回 false，否则比对商家主键。
     * 业务规则：商家账号查看本店订单视为参与方。
     * 注意事项：merchantService 返回 null 时按非参与方处理。
     */
    private boolean isMerchantParticipant(Long userId, Long merchantId) {
        if (userId == null || merchantId == null) {
            return false;
        }
        Merchant merchant = merchantService.findByUserId(userId);
        return merchant != null && merchantId.equals(merchant.getId_wsh());
    }

}
