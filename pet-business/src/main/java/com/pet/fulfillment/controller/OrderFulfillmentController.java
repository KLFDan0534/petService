package com.pet.fulfillment.controller;

import com.pet.common.Result;
import com.pet.customer.entity.ChatMessage;
import com.pet.fulfillment.dto.CreateCareRecordRequest;
import com.pet.fulfillment.dto.SendOrderMessageRequest;
import com.pet.fulfillment.service.OrderFulfillmentService;
import com.pet.pet.entity.CareRecord;
import com.pet.security.JwtAuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 订单履行控制器
 * 提供订单护理时间线、每日上传状态及订单聊天功能
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@RestController
@RequestMapping("/api/order-fulfillments")
@Tag(name = "订单履行", description = "订单护理时间线、照片和订单范围内的聊天")
@Slf4j
public class OrderFulfillmentController {

    private final OrderFulfillmentService fulfillmentService;

    public OrderFulfillmentController(OrderFulfillmentService fulfillmentService) {
        this.fulfillmentService = fulfillmentService;
    }

    /**
     * 获取订单履行概览
     * @param token 当前用户认证信息
     * @param orderId 订单ID
     * @return 履行概览信息
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取订单履行概览")
    public Result<Map<String, Object>> overview(@AuthenticationPrincipal JwtAuthenticationToken token,
                                                @PathVariable Long orderId) {
        log.info("调用 overview()");
        return Result.success(fulfillmentService.getOverview(token.getUserId(), isAdmin(token), orderId));
    }

    /**
     * 获取订单护理时间线
     * @param token 当前用户认证信息
     * @param orderId 订单ID
     * @return 护理记录列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/{orderId}/timeline")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取订单护理时间线")
    public Result<List<CareRecord>> timeline(@AuthenticationPrincipal JwtAuthenticationToken token,
                                             @PathVariable Long orderId) {
        log.info("调用 timeline()");
        return Result.success(fulfillmentService.listTimeline(token.getUserId(), isAdmin(token), orderId));
    }

    /**
     * 获取每日护理上传状态
     * @param token 当前用户认证信息
     * @param orderId 订单ID
     * @return 每日上传状态信息
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/{orderId}/daily-status")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取每日护理上传状态")
    public Result<Map<String, Object>> dailyStatus(@AuthenticationPrincipal JwtAuthenticationToken token,
                                                   @PathVariable Long orderId) {
        log.info("调用 dailyStatus()");
        return Result.success(fulfillmentService.getDailyUploadStatus(token.getUserId(), isAdmin(token), orderId));
    }

    /**
     * 创建订单护理时间线记录
     * @param token 当前用户认证信息
     * @param orderId 订单ID
     * @param request 创建护理记录请求
     * @return 创建的护理记录
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/{orderId}/timeline")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "创建护理时间线记录")
    public Result<CareRecord> createTimeline(@AuthenticationPrincipal JwtAuthenticationToken token,
                                             @PathVariable Long orderId,
                                             @RequestBody CreateCareRecordRequest request) {
        log.info("调用 createTimeline()");
        return Result.success(fulfillmentService.createTimelineRecord(token.getUserId(), isAdmin(token), orderId, request));
    }

    /**
     * 创建护理时间线记录并上传图片
     * @param token 当前用户认证信息
     * @param orderId 订单ID
     * @param typeWsh 护理类型
     * @param type 护理类型（备用字段名）
     * @param contentWsh 护理内容
     * @param content 护理内容（备用字段名）
     * @param recordTimeWsh 记录时间
     * @param recordTime 记录时间（备用字段名）
     * @param files 上传文件数组
     * @param file 单个上传文件
     * @return 创建的护理记录
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping(value = "/{orderId}/timeline/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "创建护理时间线记录并上传照片")
    public Result<CareRecord> uploadTimeline(@AuthenticationPrincipal JwtAuthenticationToken token,
                                             @PathVariable Long orderId,
                                             @RequestParam(value = "type_wsh", required = false) String typeWsh,
                                             @RequestParam(value = "type", required = false) String type,
                                             @RequestParam(value = "content_wsh", required = false) String contentWsh,
                                             @RequestParam(value = "content", required = false) String content,
                                             @RequestParam(value = "record_time_wsh", required = false) String recordTimeWsh,
                                             @RequestParam(value = "recordTime", required = false) String recordTime,
                                             @RequestParam(value = "files", required = false) MultipartFile[] files,
                                             @RequestParam(value = "file", required = false) MultipartFile file) {
        log.info("调用 uploadTimeline()");
        return Result.success(fulfillmentService.createTimelineRecordWithFiles(
                token.getUserId(), isAdmin(token), orderId,
                firstNonBlank(typeWsh, type),
                firstNonBlank(contentWsh, content),
                firstNonBlank(recordTimeWsh, recordTime),
                mergeFiles(files, file)));
    }

    /**
     * 获取订单范围内的会话消息
     * @param token 当前用户认证信息
     * @param orderId 订单ID
     * @param otherUserId 对方用户ID（可选）
     * @return 会话消息列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/{orderId}/conversation")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取订单会话消息")
    public Result<List<ChatMessage>> conversation(@AuthenticationPrincipal JwtAuthenticationToken token,
                                                  @PathVariable Long orderId,
                                                  @RequestParam(required = false) Long otherUserId) {
        log.info("调用 conversation()");
        return Result.success(fulfillmentService.listConversation(token.getUserId(), isAdmin(token), orderId, otherUserId));
    }

    /**
     * 发送订单范围内的消息
     * @param token 当前用户认证信息
     * @param orderId 订单ID
     * @param request 发送消息请求
     * @return 发送的消息
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/{orderId}/conversation")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "发送订单消息")
    public Result<ChatMessage> sendMessage(@AuthenticationPrincipal JwtAuthenticationToken token,
                                           @PathVariable Long orderId,
                                           @RequestBody SendOrderMessageRequest request) {
        log.info("调用 sendMessage()");
        return Result.success(fulfillmentService.sendMessage(token.getUserId(), isAdmin(token), orderId, request));
    }

    /**
     * 发送订单范围内带文件的消息
     * @param token 当前用户认证信息
     * @param orderId 订单ID
     * @param toUserIdWsh 接收方用户ID
     * @param toUserId 接收方用户ID（备用字段名）
     * @param contentWsh 消息内容
     * @param content 消息内容（备用字段名）
     * @param typeWsh 消息类型
     * @param type 消息类型（备用字段名）
     * @param file 上传文件
     * @return 发送的消息
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping(value = "/{orderId}/conversation/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "发送带文件的订单消息")
    public Result<ChatMessage> sendMessageWithFile(@AuthenticationPrincipal JwtAuthenticationToken token,
                                                   @PathVariable Long orderId,
                                                   @RequestParam(value = "to_user_id_wsh", required = false) Long toUserIdWsh,
                                                   @RequestParam(value = "toUserId", required = false) Long toUserId,
                                                   @RequestParam(value = "content_wsh", required = false) String contentWsh,
                                                   @RequestParam(value = "content", required = false) String content,
                                                   @RequestParam(value = "type_wsh", required = false) String typeWsh,
                                                   @RequestParam(value = "type", required = false) String type,
                                                   @RequestParam("file") MultipartFile file) {
        log.info("调用 sendMessageWithFile()");
        return Result.success(fulfillmentService.sendMessageWithFile(
                token.getUserId(), isAdmin(token), orderId,
                toUserIdWsh != null ? toUserIdWsh : toUserId,
                firstNonBlank(contentWsh, content),
                firstNonBlank(typeWsh, type),
                file));
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
}
