package com.pet.customer.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.common.PageRequestDTO;
import com.pet.customer.dto.TicketCreateRequestDTO;
import com.pet.customer.dto.TicketDTO;
import com.pet.customer.dto.TicketListRequestDTO;
import com.pet.customer.dto.TicketMessageDTO;

import java.util.List;

/**
 * 【业务模块】工单售后管理
 * 业务作用：提供用户提交售后/客服工单、内部人员分配、处理和关闭的全生命周期管理。
 * 工单支持优先级、分类、消息回复和状态流转（pending -> processing -> resolved -> closed）。
 */
public interface TicketService {
    /**
     * 查询用户发起的工单列表
     *
     * 业务作用：根据用户 ID 获取其发起的工单列表，按创建时间倒序。
     * 注意事项：包含所有状态的工单。
     *
     * @param userId 用户ID
     * @return 工单DTO列表，按创建时间倒序
     */
    List<TicketDTO> listByUser(Long userId);

    /**
     * 分页查询工单列表
     *
     * 业务作用：分页查询全部工单。
     *
     * @param pageParam 分页参数
     * @return 工单分页数据，按创建时间倒序
     */
    IPage<TicketDTO> listPage(PageRequestDTO pageParam);

    /**
     * 按角色权限分页查询工单列表
     *
     * 业务作用：按角色权限分页查询工单列表。
     * 业务规则：角色权限隔离。
     *
     * @param pageParam 分页参数（含筛选条件：状态/分类/优先级/处理人/关键字）
     * @param staffUserId 当前操作用户ID
     * @param admin 是否为管理员
     * @param merchant 是否为商家
     * @param customerService 是否为客服
     * @return 过滤后的工单分页数据
     */
    IPage<TicketDTO> listPageForStaff(TicketListRequestDTO pageParam, Long staffUserId, boolean admin, boolean merchant, boolean customerService);

    /**
     * 按角色权限查询工单详情
     *
     * 业务作用：根据角色权限查询工单详情，无权限时返回错误。
     * 业务规则：权限隔离。
     * 异常情况：无权限抛 BusinessException。
     *
     * @param id 工单ID
     * @param userId 当前用户ID
     * @param admin 是否为管理员
     * @param merchant 是否为商家
     * @param customerService 是否为客服
     * @return 工单DTO
     */
    TicketDTO getByIdForUser(Long id, Long userId, boolean admin, boolean merchant, boolean customerService);

    /**
     * 创建工单
     *
     * 业务作用：用户提交售后/客服工单，可关联订单或商家，初始状态为 pending。
     *
     * @param userId 用户ID
     * @param request 工单创建请求
     * @return 创建成功的工单DTO
     */
    TicketDTO create(Long userId, TicketCreateRequestDTO request);

    /**
     * 分配工单
     *
     * 业务作用：分配工单给处理人，状态从 pending → processing。
     * 业务规则：仅 pending 状态可分配。
     * 异常情况：非 pending 状态抛异常。
     *
     * @param id 工单ID
     * @param assigneeId 处理人用户ID
     * @return 更新后的工单DTO
     */
    TicketDTO assign(Long id, Long assigneeId);

    /**
     * 按角色权限分配工单
     *
     * 业务作用：按角色权限分配工单给处理人。
     * 业务规则：权限隔离。
     * 异常情况：无权限抛异常。
     *
     * @param id 工单ID
     * @param assigneeId 处理人用户ID
     * @param staffUserId 当前操作用户ID
     * @param admin 是否为管理员
     * @param merchant 是否为商家
     * @param customerService 是否为客服
     * @return 更新后的工单DTO
     */
    TicketDTO assignForStaff(Long id, Long assigneeId, Long staffUserId, boolean admin, boolean merchant, boolean customerService);

    /**
     * 解决工单
     *
     * 业务作用：解决 pending 或 processing 状态的工单，状态变为 resolved。
     * 业务规则：仅 pending 或 processing 状态可解决。
     * 异常情况：状态不匹配抛异常。
     * 注意事项：自动添加一条解决消息。
     *
     * @param id 工单ID
     * @param result 处理结果描述
     * @return 更新后的工单DTO
     */
    TicketDTO resolve(Long id, String result);

    /**
     * 按角色权限解决工单
     *
     * 业务作用：按角色权限解决工单。
     * 业务规则：权限隔离。
     * 异常情况：无权限抛异常。
     *
     * @param id 工单ID
     * @param result 处理结果描述
     * @param staffUserId 当前操作用户ID
     * @param admin 是否为管理员
     * @param merchant 是否为商家
     * @param customerService 是否为客服
     * @return 更新后的工单DTO
     */
    TicketDTO resolveForStaff(Long id, String result, Long staffUserId, boolean admin, boolean merchant, boolean customerService);

    /**
     * 关闭工单
     *
     * 业务作用：关闭已解决的工单，状态变为 closed。
     * 业务规则：仅 resolved 状态可关闭。
     * 异常情况：非 resolved 状态抛异常。
     *
     * @param id 工单ID
     * @return 更新后的工单DTO
     */
    TicketDTO close(Long id);

    /**
     * 按角色权限关闭工单
     *
     * 业务作用：按角色权限关闭工单。
     * 业务规则：权限隔离。
     * 异常情况：无权限抛异常。
     *
     * @param id 工单ID
     * @param staffUserId 当前操作用户ID
     * @param admin 是否为管理员
     * @param merchant 是否为商家
     * @param customerService 是否为客服
     * @return 更新后的工单DTO
     */
    TicketDTO closeForStaff(Long id, Long staffUserId, boolean admin, boolean merchant, boolean customerService);

    /**
     * 添加工单消息
     *
     * 业务作用：向工单中添加一条消息记录。
     * 异常情况：工单不存在抛异常。
     * 注意事项：不改变工单状态。
     *
     * @param ticketId 工单ID
     * @param userId 发送消息的用户ID
     * @param content 消息内容
     * @return 创建的消息DTO
     */
    TicketMessageDTO addMessage(Long ticketId, Long userId, String content, String fileUrl);

    /**
     * 按角色权限添加工单消息
     *
     * 业务作用：按角色权限向工单中添加消息。
     * 业务规则：权限隔离。
     * 异常情况：无权限抛异常。
     *
     * @param ticketId 工单ID
     * @param userId 发送消息的用户ID
     * @param admin 是否为管理员
     * @param merchant 是否为商家
     * @param customerService 是否为客服
     * @param content 消息内容
     * @return 创建的消息DTO
     */
    TicketMessageDTO addMessageForUser(Long ticketId, Long userId, boolean admin, boolean merchant, boolean customerService, String content, String fileUrl);

    /**
     * 获取工单留言列表
     *
     * 业务作用：获取工单的所有留言，按创建时间正序排列。
     *
     * @param ticketId 工单ID
     * @return 消息DTO列表，按创建时间正序
     */
    List<TicketMessageDTO> listMessages(Long ticketId);

    /**
     * 按角色权限获取工单留言列表
     *
     * 业务作用：按角色权限获取工单留言列表。
     * 业务规则：权限隔离。
     * 异常情况：无权限抛异常。
     *
     * @param ticketId 工单ID
     * @param userId 当前用户ID
     * @param admin 是否为管理员
     * @param merchant 是否为商家
     * @param customerService 是否为客服
     * @return 消息DTO列表，按创建时间正序
     */
    List<TicketMessageDTO> listMessagesForUser(Long ticketId, Long userId, boolean admin, boolean merchant, boolean customerService);
}
