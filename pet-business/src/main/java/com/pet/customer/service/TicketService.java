package com.pet.customer.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.common.PageRequestDTO;
import com.pet.customer.dto.TicketCreateRequestDTO;
import com.pet.customer.dto.TicketDTO;
import com.pet.customer.dto.TicketMessageDTO;

import java.util.List;

/**
 * 【业务模块】工单售后管理
 * 业务作用：提供用户提交售后/客服工单、内部人员分配、处理和关闭的全生命周期管理。
 * 工单支持优先级、分类、消息回复和状态流转（pending -> processing -> resolved -> closed）。
 */
public interface TicketService {
    /**
     * 【业务名称】查询用户发起的工单列表
     * 业务作用：根据用户 ID 获取其发起的工单列表，按创建时间倒序。
     * 调用场景：用户查看自己的工单记录。
     * 调用链：TicketService.listByUser() → TicketMapper.selectList()。
     * 数据处理：按 user_id 匹配，按创建时间倒序。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：包含所有状态的工单。
     *
     * @param userId 用户ID
     * @return 工单DTO列表，按创建时间倒序
     */
    List<TicketDTO> listByUser(Long userId);

    /**
     * 【业务名称】查询全部工单列表
     * 业务作用：获取所有工单列表（管理员用），按创建时间倒序。
     * 调用场景：后台管理工单列表。
     * 调用链：TicketService.listAll() → TicketMapper.selectList()。
     * 数据处理：无条件全量查询，按创建时间倒序。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     *
     * @return 全部工单DTO列表，按创建时间倒序
     */
    List<TicketDTO> listAll();

    /**
     * 【业务名称】分页查询工单列表
     * 业务作用：分页查询全部工单。
     * 调用场景：后台分页管理工单。
     * 调用链：TicketService.listPage() → TicketMapper.selectPage()。
     * 数据处理：分页查询，按创建时间倒序。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     *
     * @param pageParam 分页参数
     * @return 工单分页数据，按创建时间倒序
     */
    IPage<TicketDTO> listPage(PageRequestDTO pageParam);

    /**
     * 【业务名称】按角色权限分页查询工单列表
     * 业务作用：按角色权限分页查询工单列表。
     * 调用场景：不同角色（管理员/商家/客服）查看工单列表。
     * 调用链：TicketService.listPageForStaff() → 确定角色可见范围 → TicketMapper.selectPage()。
     * 数据处理：管理员查全部；商家只能查自己商家相关的工单；客服可查其服务商家的工单。
     * 业务规则：角色权限隔离。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     *
     * @param pageParam 分页参数
     * @param staffUserId 当前操作用户ID
     * @param admin 是否为管理员
     * @param merchant 是否为商家
     * @param customerService 是否为客服
     * @return 过滤后的工单分页数据
     */
    IPage<TicketDTO> listPageForStaff(PageRequestDTO pageParam, Long staffUserId, boolean admin, boolean merchant, boolean customerService);

    /**
     * 【业务名称】查询工单详情
     * 业务作用：根据ID查询工单详情。
     * 调用场景：查看工单详细信息。
     * 调用链：TicketService.getById() → TicketMapper.selectById()。
     * 数据处理：按ID精确查询。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：工单不存在时抛异常。
     * 注意事项：无。
     *
     * @param id 工单ID
     * @return 工单DTO
     */
    TicketDTO getById(Long id);

    /**
     * 【业务名称】按角色权限查询工单详情
     * 业务作用：根据角色权限查询工单详情，无权限时返回错误。
     * 调用场景：内部人员查看工单详情。
     * 调用链：TicketService.getByIdForUser() → 权限校验 → TicketMapper.selectById()。
     * 数据处理：同 getById()，增加权限校验。
     * 业务规则：权限隔离。
     * 状态影响：无。
     * 异常情况：无权限抛 BusinessException。
     * 注意事项：无。
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
     * 【业务名称】创建工单
     * 业务作用：用户提交售后/客服工单，可关联订单或商家，初始状态为 pending。
     * 调用场景：用户提交售后客服需求。
     * 调用链：TicketService.create() → 构造实体 → TicketMapper.insert()。
     * 数据处理：创建 pending 状态工单，默认优先级 medium。
     * 业务规则：无。
     * 状态影响：新增一条 pending 状态工单记录。
     * 异常情况：无。
     * 注意事项：无。
     *
     * @param userId 用户ID
     * @param request 工单创建请求
     * @return 创建成功的工单DTO
     */
    TicketDTO create(Long userId, TicketCreateRequestDTO request);

    /**
     * 【业务名称】分配工单
     * 业务作用：分配工单给处理人，状态从 pending → processing。
     * 调用场景：后台分配工单给客服。
     * 调用链：TicketService.assign() → 校验状态 → TicketMapper.updateById()。
     * 数据处理：更新 assignee_id 和状态。
     * 业务规则：仅 pending 状态可分配。
     * 状态影响：工单状态 pending → processing。
     * 异常情况：非 pending 状态抛异常。
     * 注意事项：无。
     *
     * @param id 工单ID
     * @param assigneeId 处理人用户ID
     * @return 更新后的工单DTO
     */
    TicketDTO assign(Long id, Long assigneeId);

    /**
     * 【业务名称】按角色权限分配工单
     * 业务作用：按角色权限分配工单给处理人。
     * 调用场景：内部人员分配工单。
     * 调用链：TicketService.assignForStaff() → 权限校验 → assign()。
     * 数据处理：同 assign()，增加权限校验。
     * 业务规则：权限隔离。
     * 状态影响：同 assign()。
     * 异常情况：无权限抛异常。
     * 注意事项：无。
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
     * 【业务名称】解决工单
     * 业务作用：解决 pending 或 processing 状态的工单，状态变为 resolved。
     * 调用场景：客服处理完成工单。
     * 调用链：TicketService.resolve() → 校验状态 → 更新 → 添加解决消息。
     * 数据处理：更新状态和结果，自动添加解决消息。
     * 业务规则：仅 pending 或 processing 状态可解决。
     * 状态影响：工单状态 → resolved。
     * 异常情况：状态不匹配抛异常。
     * 注意事项：自动添加一条解决消息。
     *
     * @param id 工单ID
     * @param result 处理结果描述
     * @return 更新后的工单DTO
     */
    TicketDTO resolve(Long id, String result);

    /**
     * 【业务名称】按角色权限解决工单
     * 业务作用：按角色权限解决工单。
     * 调用场景：内部人员解决工单。
     * 调用链：TicketService.resolveForStaff() → 权限校验 → resolve()。
     * 数据处理：同 resolve()，增加权限校验。
     * 业务规则：权限隔离。
     * 状态影响：同 resolve()。
     * 异常情况：无权限抛异常。
     * 注意事项：无。
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
     * 【业务名称】关闭工单
     * 业务作用：关闭已解决的工单，状态变为 closed。
     * 调用场景：用户或管理员关闭工单。
     * 调用链：TicketService.close() → 校验状态 → TicketMapper.updateById()。
     * 数据处理：更新状态为 closed。
     * 业务规则：仅 resolved 状态可关闭。
     * 状态影响：工单状态 resolved → closed。
     * 异常情况：非 resolved 状态抛异常。
     * 注意事项：无。
     *
     * @param id 工单ID
     * @return 更新后的工单DTO
     */
    TicketDTO close(Long id);

    /**
     * 【业务名称】按角色权限关闭工单
     * 业务作用：按角色权限关闭工单。
     * 调用场景：内部人员关闭工单。
     * 调用链：TicketService.closeForStaff() → 权限校验 → close()。
     * 数据处理：同 close()，增加权限校验。
     * 业务规则：权限隔离。
     * 状态影响：同 close()。
     * 异常情况：无权限抛异常。
     * 注意事项：无。
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
     * 【业务名称】添加工单消息
     * 业务作用：向工单中添加一条消息记录。
     * 调用场景：用户或客服在工单中留言。
     * 调用链：TicketService.addMessage() → TicketMessageMapper.insert()。
     * 数据处理：插入消息记录。
     * 业务规则：无。
     * 状态影响：新增一条消息记录。
     * 异常情况：工单不存在抛异常。
     * 注意事项：不改变工单状态。
     *
     * @param ticketId 工单ID
     * @param userId 发送消息的用户ID
     * @param content 消息内容
     * @return 创建的消息DTO
     */
    TicketMessageDTO addMessage(Long ticketId, Long userId, String content);

    /**
     * 【业务名称】按角色权限添加工单消息
     * 业务作用：按角色权限向工单中添加消息。
     * 调用场景：内部人员在工单中留言。
     * 调用链：TicketService.addMessageForUser() → 权限校验 → addMessage()。
     * 数据处理：同 addMessage()，增加权限校验。
     * 业务规则：权限隔离。
     * 状态影响：同 addMessage()。
     * 异常情况：无权限抛异常。
     * 注意事项：无。
     *
     * @param ticketId 工单ID
     * @param userId 发送消息的用户ID
     * @param admin 是否为管理员
     * @param merchant 是否为商家
     * @param customerService 是否为客服
     * @param content 消息内容
     * @return 创建的消息DTO
     */
    TicketMessageDTO addMessageForUser(Long ticketId, Long userId, boolean admin, boolean merchant, boolean customerService, String content);

    /**
     * 【业务名称】获取工单留言列表
     * 业务作用：获取工单的所有留言，按创建时间正序排列。
     * 调用场景：查看工单沟通记录。
     * 调用链：TicketService.listMessages() → TicketMessageMapper.selectList()。
     * 数据处理：按 ticket_id 匹配，按创建时间正序。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     *
     * @param ticketId 工单ID
     * @return 消息DTO列表，按创建时间正序
     */
    List<TicketMessageDTO> listMessages(Long ticketId);

    /**
     * 【业务名称】按角色权限获取工单留言列表
     * 业务作用：按角色权限获取工单留言列表。
     * 调用场景：内部人员查看工单沟通记录。
     * 调用链：TicketService.listMessagesForUser() → 权限校验 → listMessages()。
     * 数据处理：同 listMessages()，增加权限校验。
     * 业务规则：权限隔离。
     * 状态影响：无。
     * 异常情况：无权限抛异常。
     * 注意事项：无。
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
