package com.pet.fulfillment.service;

import com.pet.customer.entity.ChatMessage;
import com.pet.fulfillment.dto.CreateCareRecordRequest;
import com.pet.fulfillment.dto.SendOrderMessageRequest;
import com.pet.order.entity.PetOrder;
import com.pet.pet.entity.CareRecord;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 订单履行服务接口
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
public interface OrderFulfillmentService {
    /**
     * 获取订单履约概览
     * @param userId 用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @return 概览数据Map
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Map<String, Object> getOverview(Long userId, boolean admin, Long orderId);

    /**
     * 获取订单护理时间线列表
     * @param userId 用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @return 护理记录列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<CareRecord> listTimeline(Long userId, boolean admin, Long orderId);

    /**
     * 获取每日上传状态
     * @param userId 用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @return 每日上传状态Map
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Map<String, Object> getDailyUploadStatus(Long userId, boolean admin, Long orderId);

    /**
     * 创建时间线护理记录
     * @param userId 用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @param request 创建护理记录请求
     * @return 创建的护理记录
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    CareRecord createTimelineRecord(Long userId, boolean admin, Long orderId, CreateCareRecordRequest request);

    /**
     * 创建时间线护理记录（含文件上传）
     * @param userId 用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @param type 记录类型
     * @param content 记录内容
     * @param recordTime 记录时间
     * @param files 上传文件列表
     * @return 创建的护理记录
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    CareRecord createTimelineRecordWithFiles(Long userId, boolean admin, Long orderId,
                                              String type, String content, String recordTime,
                                              List<MultipartFile> files);

    /**
     * 获取订单聊天对话列表
     * @param userId 用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @param otherUserId 对方用户ID
     * @return 聊天消息列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<ChatMessage> listConversation(Long userId, boolean admin, Long orderId, Long otherUserId);

    /**
     * 发送订单消息
     * @param userId 用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @param request 发送消息请求
     * @return 发送后的聊天消息
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    ChatMessage sendMessage(Long userId, boolean admin, Long orderId, SendOrderMessageRequest request);

    /**
     * 发送订单消息（含文件）
     * @param userId 用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @param toUserId 接收方用户ID
     * @param content 消息内容
     * @param type 消息类型
     * @param file 上传文件
     * @return 发送后的聊天消息
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    ChatMessage sendMessageWithFile(Long userId, boolean admin, Long orderId,
                                    Long toUserId, String content, String type, MultipartFile file);

    /**
     * 校验并获取可读订单
     * @param userId 用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @return 订单实体
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    PetOrder requireReadableOrder(Long userId, boolean admin, Long orderId);
}
