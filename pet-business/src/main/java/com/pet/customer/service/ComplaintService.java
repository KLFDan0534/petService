package com.pet.customer.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.common.PageRequestDTO;
import com.pet.customer.dto.ComplaintCreateRequestDTO;
import com.pet.customer.dto.ComplaintDTO;
import com.pet.customer.dto.ComplaintEvidenceDTO;
import com.pet.customer.entity.Complaint;

import java.util.List;

/**
 * 【业务模块】投诉管理
 * 业务作用：提供用户对商家/照看者的投诉创建、查询、审核处理全流程管理。
 * 投诉可关联订单，并自动汇聚订单维度的聊天记录和护理记录作为证据。
 */
public interface ComplaintService {
    /**
     * 【业务名称】按投诉人查询投诉列表
     * 业务作用：根据投诉人 ID 获取其发起的投诉列表，按创建时间倒序。
     * 调用场景：用户查看自己发起的投诉。
     * 调用链：ComplaintService.listByOwner() → ComplaintMapper.selectList() → toDTOList()。
     * 数据处理：按 owner_id 匹配，按创建时间倒序。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：仅返回当前投诉人的投诉。
     *
     * @param ownerId 投诉人（宠物主）用户ID
     * @return 该用户的投诉列表，按创建时间倒序
     */
    List<ComplaintDTO> listByOwner(Long ownerId);

    /**
     * 【业务名称】查询全部投诉
     * 业务作用：获取所有投诉记录（管理员用），按创建时间倒序。
     * 调用场景：后台管理投诉列表。
     * 调用链：ComplaintService.listAll() → ComplaintMapper.selectList()。
     * 数据处理：无条件全量查询。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     *
     * @return 全部投诉列表，按创建时间倒序
     */
    List<ComplaintDTO> listAll();

    /**
     * 【业务名称】分页查询投诉
     * 业务作用：分页查询全部投诉列表。
     * 调用场景：后台分页管理投诉。
     * 调用链：ComplaintService.listPage() → ComplaintMapper.selectPage()。
     * 数据处理：分页查询，按创建时间倒序。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     *
     * @param pageParam 分页参数（页码、每页条数）
     * @return 投诉分页数据
     */
    IPage<ComplaintDTO> listPage(PageRequestDTO pageParam);

    /**
     * 【业务名称】按角色分页查询投诉
     * 业务作用：按角色权限分页查询投诉列表。
     * 调用场景：不同角色（管理员/商家/客服）查看投诉列表。
     * 调用链：ComplaintService.listPageForStaff() → staffMerchantIds() → ComplaintMapper.selectPage()。
     * 数据处理：管理员查全部；商家只能查自己商家的投诉；客服可查其服务商家的投诉。
     * 业务规则：管理员可查看全部；商家只能看到自己商家的投诉；客服可看到其服务的所有商家的投诉。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：角色权限隔离。
     *
     * @param pageParam 分页参数
     * @param staffUserId 当前操作用户ID
     * @param admin 是否为管理员
     * @param merchant 是否为商家
     * @param customerService 是否为客服
     * @return 过滤后的投诉分页数据
     */
    IPage<ComplaintDTO> listPageForStaff(PageRequestDTO pageParam, Long staffUserId,
                                         boolean admin, boolean merchant, boolean customerService);

    /**
     * 【业务名称】获取投诉证据详情
     * 业务作用：获取投诉证据详情，包含投诉基本信息、订单摘要、关联的聊天记录和护理记录。
     * 调用场景：用户查看投诉处理详情。
     * 调用链：ComplaintService.getEvidence() → buildEvidence()。
     * 数据处理：组装投诉信息、订单摘要、聊天记录和护理记录。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：投诉不存在时抛异常。
     * 注意事项：证据详情自动汇聚订单维度的聊天记录和护理记录。
     *
     * @param id 投诉ID
     * @return 投诉证据详情
     */
    ComplaintEvidenceDTO getEvidence(Long id);

    /**
     * 【业务名称】按角色获取投诉证据详情
     * 业务作用：按角色权限获取投诉证据详情。
     * 调用场景：内部人员（管理员/商家/客服）审核投诉。
     * 调用链：ComplaintService.getEvidenceForStaff() → assertStaffCanManage() → buildEvidence()。
     * 数据处理：同 getEvidence()，增加权限校验。
     * 业务规则：无权限时抛 BusinessException。
     * 状态影响：无。
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
     * 【业务名称】创建投诉
     * 业务作用：用户对指定订单（或商家）发起投诉，校验订单归属和商家有效性。
     * 调用场景：用户提交投诉。
     * 调用链：ComplaintService.create() → validateOrderComplaint() → resolveMerchantId() → insert() → toDTO()。
     * 数据处理：校验→解析目标→插入 pending 状态投诉。
     * 业务规则：需校验订单归属和商家有效性；创建后状态为 pending。
     * 状态影响：新增一条 pending 状态投诉记录。
     * 异常情况：订单不存在抛 BusinessException(404)；非订单主人抛 BusinessException(403)；商家不存在抛异常。
     * 注意事项：投诉可关联订单，初始状态为 pending。
     *
     * @param request 投诉创建请求，包含订单ID、标题、内容、图片等
     * @param ownerId 投诉人（宠物主）用户ID
     * @return 创建成功的投诉DTO
     */
    ComplaintDTO create(ComplaintCreateRequestDTO request, Long ownerId);

    /**
     * 【业务名称】处理投诉（管理员）
     * 业务作用：处理投诉，更新状态和结果，发送站内通知和异步消息。
     * 调用场景：后台管理处理投诉。
     * 调用链：ComplaintService.process() → processLoaded() → 更新状态 → sendComplaintNotification() → messageSender.sendComplaintProcess()。
     * 数据处理：更新状态和结果 → 发送通知 → 事务提交后异步发送消息。
     * 业务规则：仅管理员可调用。
     * 状态影响：投诉状态变更；发送站内通知给投诉人。
     * 异常情况：投诉不存在时抛异常。
     * 注意事项：事务提交后异步发送消息到 MQ。
     *
     * @param id 投诉ID
     * @param result 处理结果描述
     * @param status 处理状态（如 "resolved" / "rejected"）
     * @return 更新后的投诉DTO
     */
    ComplaintDTO process(Long id, String result, String status);

    /**
     * 【业务名称】按角色处理投诉
     * 业务作用：按角色权限处理投诉。
     * 调用场景：内部人员（管理员/商家/客服）处理投诉。
     * 调用链：ComplaintService.processForStaff() → assertStaffCanManage() → processLoaded()。
     * 数据处理：同 process()，增加权限校验。
     * 业务规则：无权限时抛 BusinessException。
     * 状态影响：同 process()。
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
}
