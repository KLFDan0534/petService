package com.pet.fulfillment.controller;

import com.pet.common.Result;
import com.pet.customer.dto.ChatMessageDTO;
import com.pet.fulfillment.dto.CreateCareRecordRequestDTO;
import com.pet.fulfillment.dto.DailyStatusDTO;
import com.pet.fulfillment.dto.SendOrderMessageRequestDTO;
import com.pet.fulfillment.service.OrderFulfillmentService;
import com.pet.fulfillment.vo.OrderFulfillmentOverviewVO;
import com.pet.pet.dto.CareRecordDTO;
import com.pet.pet.entity.CareRecord;
import com.pet.security.JwtAuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/order-fulfillments")
@Tag(name = "【用户端】订单履行管理", description = "订单时间线、护理记录和订单消息（用户/看护者/商家使用）")
@Slf4j
public class OrderFulfillmentController {

    private final OrderFulfillmentService fulfillmentService;

    /**
     * 【构造方法】注入订单履行服务
     *
     * 业务作用：由 Spring 注入 OrderFulfillmentService，供各接口调用。
     *
     * 调用场景：Spring 容器启动创建 Controller Bean 时调用。
     *
     * 调用链：Spring 容器 → 本构造方法 → fulfillmentService 赋值。
     *
     * 数据处理：仅字段赋值。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：依赖缺失时由 Spring 抛出 Bean 创建异常。
     *
     * 注意事项：依赖为 final 字段。
     */
    public OrderFulfillmentController(OrderFulfillmentService fulfillmentService) {
        this.fulfillmentService = fulfillmentService;
    }

    /**
     * 【获取订单履行概览】返回订单、角色、时间线与每日上传状态
     *
     * 业务作用：对外暴露 GET /api/order-fulfillments/{orderId}，一次性返回订单履行的整体视图。
     *
     * 调用场景：订单详情页进入履行视图时前端调用。
     *
     * 调用链：前端 → 本方法 → fulfillmentService.getOverview → OrderFulfillmentServiceImpl.getOverview。
     *
     * 数据处理：从 JwtAuthenticationToken 取 userId，isAdmin 判定管理员身份，与 orderId 一起传入服务层，返回 OrderFulfillmentOverviewVO。
     *
     * 业务规则：需登录（@PreAuthorize isAuthenticated）；具体可见性由服务层 requireAccess 判定。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：未登录 401；无权限 403；订单不存在 404，均由服务层 BusinessException 转译。
     *
     * 注意事项：管理员身份通过 ROLE_ADMIN 权限字符串判断，非数据库角色表。
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取订单履行概览", description = "获取订单履行总体概览")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "资源不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<OrderFulfillmentOverviewVO> overview(@AuthenticationPrincipal JwtAuthenticationToken token,
                                                       @PathVariable @Parameter(description = "订单ID") Long orderId) {
        return Result.success(fulfillmentService.getOverview(token.getUserId(), isAdmin(token), orderId));
    }

    /**
     * 【获取护理时间线】返回订单护理记录列表
     *
     * 业务作用：对外暴露 GET /api/order-fulfillments/{orderId}/timeline，把服务层返回的实体转换为 DTO 列表。
     *
     * 调用场景：订单履行页展示护理时间线时前端调用。
     *
     * 调用链：前端 → 本方法 → fulfillmentService.listTimeline → 本类 toCareRecordDTO。
     *
     * 数据处理：List<CareRecord> → stream 映射为 List<CareRecordDTO>，按记录时间与创建时间倒序（排序在服务层完成）。
     *
     * 业务规则：需登录；可见性由服务层判定。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：未登录 401；无权限 403；订单不存在 404。
     *
     * 注意事项：控制器内又做了一次实体到 DTO 的转换，服务层已存在同逻辑方法，属重复实现。
     */
    @GetMapping("/{orderId}/timeline")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取订单护理时间线", description = "获取订单护理记录时间线")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "资源不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<CareRecordDTO>> timeline(@AuthenticationPrincipal JwtAuthenticationToken token,
                                                @PathVariable @Parameter(description = "订单ID") Long orderId) {
        // 服务层返回实体，这里再转一次 DTO
        List<CareRecord> list = fulfillmentService.listTimeline(token.getUserId(), isAdmin(token), orderId);
        return Result.success(list.stream().map(this::toCareRecordDTO).collect(Collectors.toList()));
    }

    /**
     * 【获取每日上传状态】返回服务期间每天的护理记录上传情况
     *
     * 业务作用：对外暴露 GET /api/order-fulfillments/{orderId}/daily-status。
     *
     * 调用场景：履行页展示护理记录打卡完整度时调用。
     *
     * 调用链：前端 → 本方法 → fulfillmentService.getDailyUploadStatus。
     *
     * 数据处理：直接透传服务层返回的 DailyStatusDTO（required_days_wsh、uploaded_days_wsh、missing_days_wsh、complete_wsh、record_count_wsh）。
     *
     * 业务规则：需登录；可见性由服务层判定。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：未登录 401；无权限 403；订单不存在 404。
     *
     * 注意事项：无。
     */
    @GetMapping("/{orderId}/daily-status")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取每日护理上传状态", description = "获取每日护理记录上传状态")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "资源不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<DailyStatusDTO> dailyStatus(@AuthenticationPrincipal JwtAuthenticationToken token,
                                              @PathVariable @Parameter(description = "订单ID") Long orderId) {
        return Result.success(fulfillmentService.getDailyUploadStatus(token.getUserId(), isAdmin(token), orderId));
    }

    /**
     * 【创建护理记录】以 JSON 请求体创建一条护理时间线记录
     *
     * 业务作用：对外暴露 POST /api/order-fulfillments/{orderId}/timeline。
     *
     * 调用场景：照看者/商家/管理员在履行页填写护理记录并提交时调用。
     *
     * 调用链：前端 → 本方法 → fulfillmentService.createTimelineRecord → 本类 toCareRecordDTO。
     *
     * 数据处理：CreateCareRecordRequestDTO（type_wsh、content_wsh、images_wsh、record_time_wsh）→ CareRecord → CareRecordDTO。
     *
     * 业务规则：需登录；写权限与在岗校验在服务层 requireWritableCareAccess 中完成。
     *
     * 状态影响：服务层写 care_record 表并写通知。
     *
     * 异常情况：未登录 401；参数或日期非法 400；无写权限 403；订单不存在 404。
     *
     * 注意事项：images_wsh 为逗号分隔的图片URL字符串，本接口不处理文件上传，上传请用 /timeline/upload。
     */
    @PostMapping("/{orderId}/timeline")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "创建护理记录", description = "创建订单护理时间线记录")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "资源不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<CareRecordDTO> createTimeline(@AuthenticationPrincipal JwtAuthenticationToken token,
                                                @PathVariable @Parameter(description = "订单ID") Long orderId,
                                                @RequestBody CreateCareRecordRequestDTO request) {
        return Result.success(toCareRecordDTO(fulfillmentService.createTimelineRecord(
                token.getUserId(), isAdmin(token), orderId, request)));
    }

    /**
     * 【上传照片创建护理记录】multipart 表单上传图片并生成记录
     *
     * 业务作用：对外暴露 POST /api/order-fulfillments/{orderId}/timeline/upload，兼容多套参数命名。
     *
     * 调用场景：照看者/商家在履行页拍照上传护理记录时调用。
     *
     * 调用链：前端 → 本方法 → firstNonBlank / mergeFiles → fulfillmentService.createTimelineRecordWithFiles → 本类 toCareRecordDTO。
     *
     * 数据处理：同时接受 type_wsh/type、content_wsh/content、record_time_wsh/recordTime 两套参数名，firstNonBlank 取先非空者；files 数组与单个 file 合并为一个 List。
     *
     * 业务规则：需登录；consumes 限定 multipart/form-data；写权限与在岗校验在服务层完成。
     *
     * 状态影响：服务层上传 MinIO、写 file_record、写 care_record 并写通知。
     *
     * 异常情况：未登录 401；无图片或非图片类型 400；无写权限 403；订单不存在 404。
     *
     * 注意事项：两套参数名是为兼容不同前端版本而保留，新前端应统一使用 _wsh 后缀；files 与 file 同时传时两者都会被上传。
     */
    @PostMapping(value = "/{orderId}/timeline/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "上传照片创建护理记录", description = "上传照片并创建护理时间线记录")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "资源不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<CareRecordDTO> uploadTimeline(@AuthenticationPrincipal JwtAuthenticationToken token,
                                                @PathVariable @Parameter(description = "订单ID") Long orderId,
                                                @RequestParam(value = "type_wsh", required = false) String typeWsh,
                                                @RequestParam(value = "type", required = false) String type,
                                                @RequestParam(value = "content_wsh", required = false) String contentWsh,
                                                @RequestParam(value = "content", required = false) String content,
                                                @RequestParam(value = "record_time_wsh", required = false) String recordTimeWsh,
                                                @RequestParam(value = "recordTime", required = false) String recordTime,
                                                @RequestParam(value = "files", required = false) MultipartFile[] files,
                                                @RequestParam(value = "file", required = false) MultipartFile file) {
        return Result.success(toCareRecordDTO(fulfillmentService.createTimelineRecordWithFiles(
                token.getUserId(), isAdmin(token), orderId,
                firstNonBlank(typeWsh, type),
                firstNonBlank(contentWsh, content),
                firstNonBlank(recordTimeWsh, recordTime),
                mergeFiles(files, file))));
    }

    /**
     * 【获取订单会话】按游标分页拉取订单聊天消息
     *
     * 业务作用：对外暴露 GET /api/order-fulfillments/{orderId}/conversation。
     *
     * 调用场景：订单履行页打开聊天面板或上拉加载历史消息时调用。
     *
     * 调用链：前端 → 本方法 → fulfillmentService.listConversation → OrderFulfillmentServiceImpl.listConversation → chatService.getConversation。
     *
     * 数据处理：otherUserId 可空（服务层自动推断对话对象），beforeId 为游标，size 为条数上限，返回 ChatMessageDTO 列表。
     *
     * 业务规则：需登录；会话双方必须为订单参与方（服务层校验）。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：未登录 401；对方非参与方 403；订单不存在 404。
     *
     * 注意事项：三个查询参数均为可选，不传 size 时由 chatService 内部默认值决定。
     */
    @GetMapping("/{orderId}/conversation")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取订单会话消息", description = "获取订单相关的聊天消息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "资源不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<ChatMessageDTO>> conversation(@AuthenticationPrincipal JwtAuthenticationToken token,
                                                     @PathVariable @Parameter(description = "订单ID") Long orderId,
                                                     @RequestParam(required = false) Long otherUserId,
                                                     @RequestParam(required = false) Long beforeId,
                                                     @RequestParam(required = false) Integer size) {
        return Result.success(fulfillmentService.listConversation(
                token.getUserId(), isAdmin(token), orderId, otherUserId, beforeId, size));
    }

    /**
     * 【发送订单消息】以 JSON 请求体发送文本或附件URL消息
     *
     * 业务作用：对外暴露 POST /api/order-fulfillments/{orderId}/conversation。
     *
     * 调用场景：订单聊天中发送文字消息时调用。
     *
     * 调用链：前端 → 本方法 → fulfillmentService.sendMessage → OrderFulfillmentServiceImpl.sendMessage。
     *
     * 数据处理：SendOrderMessageRequestDTO 透传给服务层，返回落库后的 ChatMessageDTO。
     *
     * 业务规则：需登录；收发双方合法性与内容非空由服务层 buildMessage 校验。
     *
     * 状态影响：服务层写 chat_message、写 notification、广播 SSE。
     *
     * 异常情况：未登录 401；参数非法 400；无权限 403；订单不存在 404。
     *
     * 注意事项：本接口不处理文件，带附件请用 /conversation/upload。
     */
    @PostMapping("/{orderId}/conversation")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "发送订单消息", description = "发送订单相关的聊天消息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "资源不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ChatMessageDTO> sendMessage(@AuthenticationPrincipal JwtAuthenticationToken token,
                                              @PathVariable @Parameter(description = "订单ID") Long orderId,
                                              @RequestBody SendOrderMessageRequestDTO request) {
        return Result.success(fulfillmentService.sendMessage(token.getUserId(), isAdmin(token), orderId, request));
    }

    /**
     * 【发送带附件消息】multipart 上传一张图片并发送消息
     *
     * 业务作用：对外暴露 POST /api/order-fulfillments/{orderId}/conversation/upload。
     *
     * 调用场景：订单聊天中发送图片时调用。
     *
     * 调用链：前端 → 本方法 → firstNonBlank → fulfillmentService.sendMessageWithFile。
     *
     * 数据处理：to_user_id_wsh 与 toUserId 二者取非空者；content_wsh/content、type_wsh/type 通过 firstNonBlank 兼容两套命名；file 为必填。
     *
     * 业务规则：需登录；consumes 限定 multipart/form-data；file 参数 required，不传会直接 400。
     *
     * 状态影响：服务层上传 MinIO、写 file_record、写 chat_message、写 notification 并广播 SSE。
     *
     * 异常情况：未登录 401；缺文件或非图片 400；无权限 403；订单不存在 404。
     *
     * 注意事项：一次只支持一个附件；接收方未传时由服务层按角色推断。
     */
    @PostMapping(value = "/{orderId}/conversation/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "发送带文件订单消息", description = "发送带附件的订单聊天消息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "资源不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ChatMessageDTO> sendMessageWithFile(@AuthenticationPrincipal JwtAuthenticationToken token,
                                                      @PathVariable @Parameter(description = "订单ID") Long orderId,
                                                      @RequestParam(value = "to_user_id_wsh", required = false) Long toUserIdWsh,
                                                      @RequestParam(value = "toUserId", required = false) Long toUserId,
                                                      @RequestParam(value = "content_wsh", required = false) String contentWsh,
                                                      @RequestParam(value = "content", required = false) String content,
                                                      @RequestParam(value = "type_wsh", required = false) String typeWsh,
                                                      @RequestParam(value = "type", required = false) String type,
                                                      @RequestParam("file") MultipartFile file) {
        return Result.success(fulfillmentService.sendMessageWithFile(
                token.getUserId(), isAdmin(token), orderId,
                toUserIdWsh != null ? toUserIdWsh : toUserId,
                firstNonBlank(contentWsh, content),
                firstNonBlank(typeWsh, type),
                file));
    }

    /**
     * 【标记会话已读】把订单聊天中对方发来的消息置为已读
     *
     * 业务作用：对外暴露 POST /api/order-fulfillments/{orderId}/conversation/read。
     *
     * 调用场景：用户打开订单聊天面板后前端调用以清空未读数。
     *
     * 调用链：前端 → 本方法 → fulfillmentService.markConversationAsRead → chatService.markConversationAsRead。
     *
     * 数据处理：otherUserId 可空，由服务层推断对方后批量更新已读状态。
     *
     * 业务规则：需登录；会话双方必须为订单参与方。
     *
     * 状态影响：更新 chat_message 的已读标记。
     *
     * 异常情况：未登录 401；无权限 403；订单不存在 404。
     *
     * 注意事项：返回 Result<Void>，无业务数据体。
     */
    @PostMapping("/{orderId}/conversation/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "标记订单会话已读", description = "标记订单聊天会话为已读")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "资源不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> markConversationRead(@AuthenticationPrincipal JwtAuthenticationToken token,
                                             @PathVariable @Parameter(description = "订单ID") Long orderId,
                                             @RequestParam(required = false) Long otherUserId) {
        fulfillmentService.markConversationAsRead(token.getUserId(), isAdmin(token), orderId, otherUserId);
        return Result.success();
    }

    /**
     * 【管理员判定】从 JWT 权限中判断是否拥有 ROLE_ADMIN
     *
     * 业务作用：为各接口计算 admin 标志并传给服务层。
     *
     * 调用场景：本类所有需要区分管理员身份的接口。
     *
     * 调用链：overview / timeline / dailyStatus / createTimeline / uploadTimeline / conversation / sendMessage / sendMessageWithFile / markConversationRead → 本方法。
     *
     * 数据处理：遍历 token.getAuthorities()，任一 authority 等于 ROLE_ADMIN 即返回 true。
     *
     * 业务规则：权限字符串必须精确等于 ROLE_ADMIN。
     *
     * 状态影响：无。
     *
     * 异常情况：token 为 null 时抛 NullPointerException，本方法未判空。
     *
     * 注意事项：与其它 Controller 中的同名私有方法逻辑重复，权限字符串硬编码。
     */
    private boolean isAdmin(JwtAuthenticationToken token) {
        return token.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    /**
     * 【取首个非空值】兼容新旧两套参数名
     *
     * 业务作用：在 _wsh 后缀参数与旧参数之间取第一个非空白值。
     *
     * 调用场景：uploadTimeline 与 sendMessageWithFile 解析表单参数时调用。
     *
     * 调用链：uploadTimeline / sendMessageWithFile → 本方法。
     *
     * 数据处理：输入两个字符串 → 输出 first（非空白时）或 second。
     *
     * 业务规则：first 为 null 或空白时回退到 second；两者皆空返回 null。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：不做 trim，返回原值；两个都为空时不会报错，交由服务层兜底或校验。
     */
    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    /**
     * 【合并上传文件】把 files 数组与单个 file 合成一个列表
     *
     * 业务作用：兼容前端既传多文件数组又传单文件两种形式。
     *
     * 调用场景：uploadTimeline 组装服务层入参时调用。
     *
     * 调用链：uploadTimeline → 本方法 → fulfillmentService.createTimelineRecordWithFiles。
     *
     * 数据处理：files 数组转 List 后追加 file（非 null 时）。
     *
     * 业务规则：files 与 file 均为空时返回空列表。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：数组中的 null 元素未过滤，由服务层 uploadFiles 跳过。
     */
    private List<MultipartFile> mergeFiles(MultipartFile[] files, MultipartFile file) {
        List<MultipartFile> merged = new ArrayList<>();
        if (files != null) {
            merged.addAll(Arrays.asList(files));
        }
        // 单文件参数与多文件数组都要合并
        if (file != null) {
            merged.add(file);
        }
        return merged;
    }

    /**
     * 【护理记录转DTO】把 CareRecord 实体字段拷贝为 CareRecordDTO
     *
     * 业务作用：控制器层输出护理记录时屏蔽实体字段。
     *
     * 调用场景：timeline、createTimeline、uploadTimeline 返回结果前调用。
     *
     * 调用链：timeline / createTimeline / uploadTimeline → 本方法。
     *
     * 数据处理：逐字段拷贝 id_wsh、order_id_wsh、pet_id_wsh、keeper_id_wsh、type_wsh、content_wsh、images_wsh、record_time_wsh、created_at_wsh。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：入参为 null 时抛 NullPointerException，未判空。
     *
     * 注意事项：与 OrderFulfillmentServiceImpl.toCareRecordDTO 完全重复，改动需两处同步。
     */
    private CareRecordDTO toCareRecordDTO(CareRecord entity) {
        CareRecordDTO dto = new CareRecordDTO();
        dto.setId_wsh(entity.getId_wsh());
        dto.setOrder_id_wsh(entity.getOrder_id_wsh());
        dto.setPet_id_wsh(entity.getPet_id_wsh());
        dto.setKeeper_id_wsh(entity.getKeeper_id_wsh());
        dto.setType_wsh(entity.getType_wsh());
        dto.setContent_wsh(entity.getContent_wsh());
        dto.setImages_wsh(entity.getImages_wsh());
        dto.setRecord_time_wsh(entity.getRecord_time_wsh());
        dto.setCreated_at_wsh(entity.getCreated_at_wsh());
        return dto;
    }
}
