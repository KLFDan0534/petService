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

    public OrderFulfillmentController(OrderFulfillmentService fulfillmentService) {
        this.fulfillmentService = fulfillmentService;
    }

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
        List<CareRecord> list = fulfillmentService.listTimeline(token.getUserId(), isAdmin(token), orderId);
        return Result.success(list.stream().map(this::toCareRecordDTO).collect(Collectors.toList()));
    }

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

    private boolean isAdmin(JwtAuthenticationToken token) {
        return token.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private List<MultipartFile> mergeFiles(MultipartFile[] files, MultipartFile file) {
        List<MultipartFile> merged = new ArrayList<>();
        if (files != null) {
            merged.addAll(Arrays.asList(files));
        }
        if (file != null) {
            merged.add(file);
        }
        return merged;
    }

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
