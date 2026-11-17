package com.pet.customer.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.common.PageRequestDTO;
import com.pet.customer.dto.ComplaintCreateRequestDTO;
import com.pet.customer.dto.ComplaintDTO;
import com.pet.customer.dto.ComplaintEvidenceDTO;
import com.pet.customer.dto.ComplaintListRequestDTO;
import com.pet.customer.dto.ComplaintMessageDTO;
import com.pet.customer.dto.ComplaintTargetsDTO;

import java.util.List;

/**
 * 【业务模块】投诉管理
 * 业务作用：提供用户对商家/照看者的投诉创建、查询、审核处理全流程管理。
 * 投诉可关联订单，并自动汇聚订单维度的聊天记录和护理记录作为证据。
 */
public interface ComplaintService {
    /**
     * 按投诉人查询投诉列表
     *
     * 业务作用：根据投诉人 ID 获取其发起的投诉列表，按创建时间倒序。
     * 注意事项：仅返回当前投诉人的投诉。
     *
     * @param ownerId 投诉人（宠物主）用户ID
     * @return 该用户的投诉列表，按创建时间倒序
     */
    List<ComplaintDTO> listByOwner(Long ownerId);

    /**
     * 分页查询投诉
     *
     * 业务作用：分页查询全部投诉列表。
     *
     * @param pageParam 分页参数（页码、每页条数）
     * @return 投诉分页数据
     */
    IPage<ComplaintDTO> listPage(PageRequestDTO pageParam);

    /**
     * 按角色分页查询投诉
     *
     * 业务作用：按角色权限分页查询投诉列表。
     * 业务规则：管理员可查看全部；商家只能看到自己商家的投诉；客服可看到其服务的所有商家的投诉。
     * 注意事项：角色权限隔离。
     *
     * @param pageParam 分页参数（含筛选条件：状态/商家/关键字）
     * @param staffUserId 当前操作用户ID
     * @param admin 是否为管理员
     * @param merchant 是否为商家
     * @param customerService 是否为客服
     * @return 过滤后的投诉分页数据
     */
    IPage<ComplaintDTO> listPageForStaff(ComplaintListRequestDTO pageParam, Long staffUserId,
                                         boolean admin, boolean merchant, boolean customerService);

    /**
     * 按角色获取投诉证据详情
     *
     * 业务作用：按角色权限获取投诉证据详情。
     * 业务规则：无权限时抛 BusinessException。
     * 异常情况：无权限时抛 BusinessException(403)。
     * 注意事项：角色权限隔离。
     *
     * @param id 投诉ID
     * @param staffUserId 当前操作用户ID
     * @param admin 是否为管理员
     * @param merchant 是否为商家
     * @param customerService 是否为客服
     * @return 投诉证据详情
     */
    ComplaintEvidenceDTO getEvidenceForStaff(Long id, Long staffUserId,
                                             boolean admin, boolean merchant, boolean customerService);

    /**
     * 获取可投诉目标列表
     *
     * 业务作用：返回当前用户可投诉/举报的对象（订单、消费过的商家、服务过的寄养师）， 前端据此渲染选择列表，用户无需手填 ID。
     * 业务规则：只返回与当前用户相关的对象。
     *
     * @param ownerId 投诉人（宠物主）用户ID
     * @return 可投诉目标列表
     */
    ComplaintTargetsDTO getTargets(Long ownerId);

    /**
     * 创建投诉
     *
     * 业务作用：用户对指定订单（或商家）发起投诉，校验订单归属和商家有效性。
     * 业务规则：需校验订单归属和商家有效性；创建后状态为 pending。
     * 异常情况：订单不存在抛 BusinessException(404)；非订单主人抛 BusinessException(403)；商家不存在抛异常。
     * 注意事项：投诉可关联订单，初始状态为 pending。
     *
     * @param request 投诉创建请求，包含订单ID、标题、内容、图片等
     * @param ownerId 投诉人（宠物主）用户ID
     * @return 创建成功的投诉DTO
     */
    ComplaintDTO create(ComplaintCreateRequestDTO request, Long ownerId);

    /**
     * 按角色处理投诉
     *
     * 业务作用：按角色权限处理投诉。
     * 业务规则：无权限时抛 BusinessException。
     * 异常情况：无权限时抛 BusinessException(403)。
     * 注意事项：角色权限隔离。
     *
     * @param id 投诉ID
     * @param result 处理结果描述
     * @param status 处理状态
     * @param staffUserId 当前操作用户ID
     * @param admin 是否为管理员
     * @param merchant 是否为商家
     * @param customerService 是否为客服
     * @return 更新后的投诉DTO
     */
    ComplaintDTO processForStaff(Long id, String result, String status, Long staffUserId,
                                 boolean admin, boolean merchant, boolean customerService);

    /**
     * 受理投诉（进入处理中）
     *
     * 业务作用：客服/管理员/商家受理投诉，状态从 pending 变为 processing，并通知投诉人。
     * 业务规则：仅 pending 状态可受理；无权限时抛 BusinessException。
     * 异常情况：无权限抛 BusinessException(403)。
     * 注意事项：角色权限隔离。
     *
     * @param id 投诉ID
     * @param staffUserId 当前操作用户ID
     * @param admin 是否为管理员
     * @param merchant 是否为商家
     * @param customerService 是否为客服
     * @return 更新后的投诉DTO
     */
    ComplaintDTO accept(Long id, Long staffUserId, boolean admin, boolean merchant, boolean customerService);

    /**
     * 查询投诉沟通消息
     *
     * 业务作用：查询某投诉的往来消息（投诉人本人或可管理该投诉的内部人员）。
     * 业务规则：投诉人本人或可管理该投诉的客服/商家/管理员可查看。
     * 异常情况：无权限抛 BusinessException(403)。
     *
     * @param id 投诉ID
     * @param userId 当前用户ID
     * @param admin 是否为管理员
     * @param merchant 是否为商家
     * @param customerService 是否为客服
     * @return 消息列表
     */
    List<ComplaintMessageDTO> listMessages(Long id, Long userId, boolean admin, boolean merchant, boolean customerService);

    /**
     * 发送投诉沟通消息
     *
     * 业务作用：投诉人或可管理该投诉的内部人员发送消息；消息会以站内通知提醒接收方。
     * 业务规则：内容必填且≤1000字；用户只能在自己的投诉中发言，客服只能在其服务商家的投诉中发言。
     * 异常情况：无权限抛 403；内容为空抛 400。
     *
     * @param id 投诉ID
     * @param senderId 发送人用户ID
     * @param content 消息内容
     * @param admin 是否为管理员
     * @param merchant 是否为商家
     * @param customerService 是否为客服
     * @return 发送成功的消息DTO
     */
    ComplaintMessageDTO sendMessage(Long id, Long senderId, String content, String fileUrl, boolean admin, boolean merchant, boolean customerService);
}
