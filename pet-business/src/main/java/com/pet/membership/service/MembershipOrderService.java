package com.pet.membership.service;

import com.pet.membership.dto.MembershipOrderCreateRequestDTO;
import com.pet.membership.dto.MembershipOrderDTO;
import com.pet.membership.entity.MembershipOrder;

import java.util.List;

/**
 * 【业务模块】会员订单管理
 * 业务作用：提供会员购买订单的创建、支付、取消、确认等全流程管理。
 * 支持幂等创建、余额支付、续费自动衔接、管理员确认支付和会员激活。
 */
public interface MembershipOrderService {
    /**
     * 【业务名称】创建会员订单
     * 业务作用：用户创建会员购买订单，通过 request_id 实现幂等。
     * 订单创建时自动计算会员有效期（续费时从现有到期日开始顺延）。
     * 调用场景：用户购买/续费会员。
     * 调用链：createOrder() → 幂等校验 → requireUsablePlan() → resolveStartAt() → insert()。
     * 数据处理：校验幂等 → 校验套餐 → 计算开始/结束时间 → 创建 pending 订单。
     * 业务规则：request_id 防重复；续费时从现有到期日顺延；状态为 pending。
     * 状态影响：新增一条 pending 订单记录；记录订单事件。
     * 异常情况：未登录抛 401；请求为空抛 400；套餐不可用抛 404/400；幂等冲突且用户不匹配抛 403。
     * 注意事项：@Transactional 保证事务一致性。
     *
     * @param userId  购买用户 ID
     * @param request 订单创建请求（含套餐、支付方式、幂等ID）
     * @return 创建成功的订单 DTO
     */
    MembershipOrderDTO createOrder(Long userId, MembershipOrderCreateRequestDTO request);

    /**
     * 【业务名称】查询我的订单列表
     * 业务作用：查询当前用户的所有会员订单，按时间倒序。
     * 调用场景：用户查看自己的购买记录。
     * 调用链：listMyOrders() → selectList() → toDTO()。
     * 数据处理：按 user_id 匹配，按创建时间倒序。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     *
     * @param userId 用户 ID
     * @return 订单 DTO 列表
     */
    List<MembershipOrderDTO> listMyOrders(Long userId);

    /**
     * 【业务名称】管理端查询订单列表
     * 业务作用：管理端查询会员订单，可选按状态筛选。
     * 调用场景：后台订单管理。
     * 调用链：listOrdersForAdmin() → selectList() → toDTO()。
     * 数据处理：按状态筛选（可选），按创建时间倒序。
     * 业务规则：状态支持 pending/paid/cancelled/refunded。
     * 状态影响：无。
     * 异常情况：不支持的状态值抛 BusinessException(400)。
     * 注意事项：无。
     *
     * @param status 可选筛选条件：pending/paid/cancelled/refunded，null 查全部
     * @return 订单 DTO 列表
     */
    List<MembershipOrderDTO> listOrdersForAdmin(String status);

    /**
     * 【业务名称】查询我的订单详情
     * 业务作用：根据订单号查询用户的订单详情（含归属校验）。
     * 调用场景：用户查看订单详情。
     * 调用链：getMyOrder() → requireOrderByNo() → toDTO()。
     * 数据处理：按订单号精确查询，校验用户归属。
     * 业务规则：仅订单所属用户可查看。
     * 状态影响：无。
     * 异常情况：订单不存在抛 404；非本人订单抛 403。
     * 注意事项：无。
     *
     * @param userId  用户 ID
     * @param orderNo 订单号
     * @return 订单 DTO
     */
    MembershipOrderDTO getMyOrder(Long userId, String orderNo);

    /**
     * 【业务名称】取消待支付订单
     * 业务作用：取消待支付的会员订单。
     * 调用场景：用户取消未支付的订单。
     * 调用链：cancelPendingOrder() → 校验归属和状态 → 乐观锁更新 → 记录事件。
     * 数据处理：乐观锁更新 status=cancelled（仅 pending 状态可取消）。
     * 业务规则：仅订单主人可取消；仅 pending 状态可取消。
     * 状态影响：订单状态 pending → cancelled；记录订单事件。
     * 异常情况：非本人抛 403；状态不可取消抛 400；并发冲突抛 400。
     * 注意事项：@Transactional 保证事务一致性。
     *
     * @param userId  用户 ID
     * @param orderNo 订单号
     * @return 取消后的订单 DTO
     */
    MembershipOrderDTO cancelPendingOrder(Long userId, String orderNo);

    /**
     * 【业务名称】支付会员订单
     * 业务作用：用户支付待支付的会员订单，余额扣款后自动激活或续费会员。
     * 调用场景：用户使用余额支付会员订单。
     * 调用链：payOrder() → 校验归属和状态 → settleBalancePayment() → 乐观锁更新 → activateMembership()。
     * 数据处理：校验 → 余额扣款 → 更新订单为 paid → 激活会员。
     * 业务规则：仅订单主人可支付；仅 pending 状态可支付；余额扣款通过 AccountingService 完成。
     * 状态影响：订单状态 pending → paid；会员 active 状态更新/新增；记录资金流水和会员事件。
     * 异常情况：非本人抛 403；状态不可支付抛 400；并发冲突抛 400。
     * 注意事项：@Transactional 保证事务一致性；会员激活使用分片锁防止并发续费出错。
     *
     * @param userId  用户 ID
     * @param orderNo 订单号
     * @return 支付成功的订单 DTO
     */
    MembershipOrderDTO payOrder(Long userId, String orderNo);

    /**
     * 【业务名称】管理端确认付费
     * 业务作用：管理员手动确认会员订单已付费（线下转账等场景），跳过余额扣款直接激活会员。
     * 调用场景：后台确认线下付费。
     * 调用链：confirmPaidForAdmin() → payOrderInternal()（skip balance）。
     * 数据处理：跳过扣款 → 更新订单为 paid → 激活会员。
     * 业务规则：仅管理员可调用。
     * 状态影响：订单状态 → paid；会员 active 状态更新/新增。
     * 异常情况：订单不存在抛异常。
     * 注意事项：@Transactional 保证事务一致性。
     *
     * @param operatorId 管理员 ID
     * @param orderNo    订单号
     * @return 确认后的订单 DTO
     */
    MembershipOrderDTO confirmPaidForAdmin(Long operatorId, String orderNo);

    /**
     * 【业务名称】订单实体转 DTO
     * 业务作用：将订单实体转换为 DTO，关联套餐快照信息。
     * 调用场景：内部转换。
     * 调用链：toDTO()。
     * 数据处理：字段拷贝 → 解析 plan_snapshot JSON 获取套餐名称和等级。
     * 业务规则：入参为 null 时返回 null。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：快照用于展示历史套餐信息。
     *
     * @param order 订单实体
     * @return 订单 DTO
     */
    MembershipOrderDTO toDTO(MembershipOrder order);
}
