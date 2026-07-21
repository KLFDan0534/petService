package com.pet.order.controller;

import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.service.MerchantService;
import com.pet.common.BusinessException;
import com.pet.common.PageResult;
import com.pet.common.Result;
import com.pet.common.annotation.LogOperation;
import com.pet.operation.entity.FileRecord;
import com.pet.operation.service.FileRecordService;
import com.pet.operation.service.impl.MinIoService;
import com.pet.order.dto.OrderCreateRequestDTO;
import com.pet.order.dto.OrderCancelRequestDTO;
import com.pet.order.dto.OrderDTO;
import com.pet.order.dto.OrderDeliveredRequestDTO;
import com.pet.order.dto.OrderProcessRequestDTO;
import com.pet.order.dto.OrderReceivedRequestDTO;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

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
        if (isAdmin(token)) {
            return Result.success(orderService.listAll());
        }
        return Result.success(orderService.listByOwner(token.getUserId()));
    }

    /**
     * 获取商户订单列表
     * @param token 当前用户认证信息
     * @param page 页码
     * @param size 每页大小
     * @return 商户订单列表及分页信息
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
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

    /**
     * 获取待处理订单列表
     * @param token 当前用户认证信息
     * @return 待处理订单列表
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @GetMapping("/pending")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取待处理订单列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<OrderDTO>> listPending(@AuthenticationPrincipal JwtAuthenticationToken token) {
        Long keeperId = findKeeperIdByUserId(token.getUserId());
        if (keeperId == null) {
            return Result.success(Collections.emptyList());
        }
        return Result.success(orderService.listPendingByKeeper(keeperId));
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
        if (!owner && !keeper && !merchant && !isAdmin(token)) {
            return Result.error(403, "无权访问此订单");
        }
        OrderDTO dto = orderService.toDTOEnriched(order);
        if (!owner && !isAdmin(token)) {
            dto.setHandover_code_wsh(null);
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
     * 接受订单
     * @param token 当前用户认证信息
     * @param body 请求体，包含orderNo
     * @return 无返回值
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @PostMapping("/accept")
    @PreAuthorize("hasRole('KEEPER') or hasRole('MERCHANT')")
    @LogOperation(module = "Order", operation = "Accept", description = "Accept order")
    @Operation(summary = "接受订单")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> accept(@AuthenticationPrincipal JwtAuthenticationToken token,
                               @Valid @RequestBody OrderProcessRequestDTO body) {
        body = requireBody(body);
        orderService.acceptOrder(token.getUserId(), body.getOrder_no_wsh());
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
     * 标记宠物已送达
     * @param token 当前用户认证信息
     * @param body 请求体，包含orderNo
     * @return 无返回值
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @PostMapping("/delivered")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(module = "Order", operation = "Delivered", description = "Mark pet delivered")
    @Operation(summary = "标记宠物已送达")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> markDelivered(@AuthenticationPrincipal JwtAuthenticationToken token,
                                      @Valid @RequestBody OrderDeliveredRequestDTO body) {
        body = requireBody(body);
        orderService.markDelivered(token.getUserId(), body);
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
     * 完成订单
     * @param token 当前用户认证信息
     * @param body 请求体，包含orderNo
     * @return 无返回值
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @PostMapping("/complete")
    @PreAuthorize("hasRole('KEEPER') or hasRole('MERCHANT')")
    @LogOperation(module = "Order", operation = "Complete", description = "Complete order")
    @Operation(summary = "完成订单")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> complete(@AuthenticationPrincipal JwtAuthenticationToken token,
                                 @Valid @RequestBody OrderProcessRequestDTO body) {
        body = requireBody(body);
        orderService.completeOrder(token.getUserId(), body.getOrder_no_wsh());
        return Result.success();
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

    private <T> T requireBody(T body) {
        if (body == null) {
            throw new BusinessException(400, "请求体不能为空");
        }
        return body;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private Long findKeeperIdByUserId(Long userId) {
        Keeper keeper = keeperMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Keeper>()
                        .eq(Keeper::getUser_id_wsh, userId)
                        .last("LIMIT 1"));
        return keeper != null ? keeper.getId_wsh() : null;
    }

    private boolean isKeeperParticipant(Long userId, Long keeperId) {
        if (userId == null || keeperId == null) {
            return false;
        }
        Long currentKeeperId = findKeeperIdByUserId(userId);
        return keeperId.equals(currentKeeperId);
    }

    private boolean isMerchantParticipant(Long userId, Long merchantId) {
        if (userId == null || merchantId == null) {
            return false;
        }
        Merchant merchant = merchantService.findByUserId(userId);
        return merchant != null && merchantId.equals(merchant.getId_wsh());
    }

    private boolean isAdmin(JwtAuthenticationToken token) {
        return token.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
