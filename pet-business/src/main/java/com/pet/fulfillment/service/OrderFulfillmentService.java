package com.pet.fulfillment.service;

import com.pet.customer.dto.ChatMessageDTO;
import com.pet.fulfillment.dto.CreateCareRecordRequestDTO;
import com.pet.fulfillment.dto.DailyStatusDTO;
import com.pet.fulfillment.vo.OrderFulfillmentOverviewVO;
import com.pet.fulfillment.dto.SendOrderMessageRequestDTO;
import com.pet.order.entity.PetOrder;
import com.pet.pet.entity.CareRecord;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 订单履行服务接口，提供宠物寄养/照看服务履行阶段的核心功能。
 * <p>包括：护理记录的上传与管理（时间线）、每日上传状态跟踪、
 * 订单参与者之间的聊天通讯、以及订单履行概览聚合。</p>
 */
public interface OrderFulfillmentService {
    /**
     * 获取订单履行概览。
     * <p>包含订单信息、当前用户角色、护理时间线记录和每日上传状态。</p>
     *
     * @param userId 当前用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @return 订单履行概览视图对象
     */
    OrderFulfillmentOverviewVO getOverview(Long userId, boolean admin, Long orderId);

    /**
     * 获取订单护理记录时间线。
     *
     * @param userId 当前用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @return 护理记录列表，按记录时间和创建时间倒序
     */
    List<CareRecord> listTimeline(Long userId, boolean admin, Long orderId);

    /**
     * 获取每日护理记录上传状态。
     * <p>对比服务期间内每一天的护理记录上传情况，
     * 返回已上传天、缺失天和完整度标识。</p>
     *
     * @param userId 当前用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @return 每日上传状态DTO
     */
    DailyStatusDTO getDailyUploadStatus(Long userId, boolean admin, Long orderId);

    /**
     * 创建护理时间线记录。
     * <p>仅照看者、商家或管理员可创建。创建时会校验记录日期在服务期间内，
     * 且照看者需处于在岗状态。</p>
     *
     * @param userId 当前用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @param request 创建护理记录请求（包含类型、内容、图片、记录时间）
     * @return 创建的护理记录实体
     */
    CareRecord createTimelineRecord(Long userId, boolean admin, Long orderId, CreateCareRecordRequestDTO request);

    /**
     * 创建护理时间线记录（含文件上传）。
     * <p>上传图片文件到 MinIO 并创建 FileRecord，然后将图片URL拼接后创建记录。</p>
     *
     * @param userId 当前用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @param type 记录类型
     * @param content 记录文本内容
     * @param recordTime 记录时间（字符串，支持 ISO 或 yyyy-MM-dd HH:mm:ss 格式）
     * @param files 上传的图片文件列表
     * @return 创建的护理记录实体
     */
    CareRecord createTimelineRecordWithFiles(Long userId, boolean admin, Long orderId,
                                              String type, String content, String recordTime,
                                              List<MultipartFile> files);

    /**
     * 获取订单聊天会话列表（使用默认分页）。
     *
     * @param userId 当前用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @param otherUserId 对方用户ID
     * @return 聊天消息DTO列表
     */
    default List<ChatMessageDTO> listConversation(Long userId, boolean admin, Long orderId, Long otherUserId) {
        return listConversation(userId, admin, orderId, otherUserId, null, null);
    }

    /**
     * 获取订单聊天会话列表（支持基于游标的分页）。
     *
     * @param userId 当前用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @param otherUserId 对方用户ID
     * @param beforeId 游标ID，返回比此ID更早的消息
     * @param size 返回条数上限
     * @return 聊天消息DTO列表
     */
    List<ChatMessageDTO> listConversation(Long userId, boolean admin, Long orderId, Long otherUserId,
                                          Long beforeId, Integer size);

    /**
     * 发送订单聊天消息。
     * <p>发送后通过 SSE 广播给收发双方，并给接收方发送站内通知。</p>
     *
     * @param userId 当前用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @param request 发送消息请求（包含接收方、内容、类型、文件URL）
     * @return 发送后的聊天消息DTO
     */
    ChatMessageDTO sendMessage(Long userId, boolean admin, Long orderId, SendOrderMessageRequestDTO request);

    /**
     * 发送订单聊天消息（含文件上传）。
     * <p>上传文件到 MinIO 后发送消息，并触发 SSE 广播和站内通知。</p>
     *
     * @param userId 当前用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @param toUserId 接收方用户ID
     * @param content 消息内容
     * @param type 消息类型
     * @param file 上传的图片文件
     * @return 发送后的聊天消息DTO
     */
    ChatMessageDTO sendMessageWithFile(Long userId, boolean admin, Long orderId,
                                    Long toUserId, String content, String type, MultipartFile file);

    /**
     * 将订单聊天会话中对方发来的消息标记为已读。
     *
     * @param userId 当前用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @param otherUserId 对方用户ID
     */
    void markConversationAsRead(Long userId, boolean admin, Long orderId, Long otherUserId);

    /**
     * 校验并获取当前用户可读的订单。
     * <p>检查当前用户是否订单的宠物主、照看者、商家或管理员，有权限则返回订单实体。</p>
     *
     * @param userId 当前用户ID
     * @param admin 是否为管理员
     * @param orderId 订单ID
     * @return 订单实体
     * @throws BusinessException 如果订单不存或用户无权限
     */
    PetOrder requireReadableOrder(Long userId, boolean admin, Long orderId);
}
