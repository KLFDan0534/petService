package com.pet.order.service;

import com.pet.order.dto.RefundDTO;
import com.pet.order.entity.Refund;
import java.util.List;

/**
 * Service interface for refund operations on pet boarding orders.
 * <p>
 * Refund lifecycle: PENDING → APPROVED → COMPLETED (or PENDING → REJECTED).
 * Refunds can only be initiated for orders in certain statuses (PAID through IN_PROGRESS).
 * A completed refund transitions the order to REFUNDED and returns funds to the owner.
 */
public interface RefundService {

    /**
     * 【查询全部退款记录】
     *
     * 业务作用：
     * 获取系统中所有退款记录，用于管理后台退款审核。
     *
     * 调用场景：
     * 管理员后台查看所有退款申请。
     *
     * 调用链：
     * 管理后台/Controller
     * ↓
     * listAll()
     * ↓
     * refundMapper.selectList(按创建时间降序)
     *
     * 状态影响：
     * 只读操作。
     *
     * @return 全部退款记录列表
     */
    List<Refund> listAll();

    /**
     * 【查询宠物主人的退款记录】
     *
     * 业务作用：
     * 根据主人ID查询其名下所有订单的退款记录，用于主人端退款进度查看。
     *
     * 调用场景：
     * 宠物主人查看"我的退款"列表。
     *
     * 调用链：
     * 主人端/Controller
     * ↓
     * listByOwner(ownerId)
     * ↓
     * orderMapper查询主人订单ID → refundMapper批量查询退款记录
     *
     * 状态影响：
     * 只读操作。
     *
     * @param ownerId 宠物主人用户ID
     * @return 退款记录列表
     */
    List<Refund> listByOwner(Long ownerId);

    /**
     * 【根据订单ID创建退款申请】
     *
     * 业务作用：
     * 校验订单归属和可退款性后委托createRefund创建退款申请，订单状态转为REFUNDING。
     *
     * 调用场景：
     * 宠物主人在订单详情页点击"申请退款"。
     *
     * 调用链：
     * 主人端/Controller
     * ↓
     * createRefundByOrderId(ownerId, orderId, reason)
     * ↓
     * orderMapper校验 → createRefund(ownerId, orderNo, reason)
     *
     * 业务规则：
     * 1. 仅订单主人可申请退款
     * 2. 可退款状态：PAID/CONFIRMED/DELIVERED/RECEIVED/IN_PROGRESS
     * 3. 不允许重复提交待审批或已通过的退款申请
     *
     * 状态影响：
     * 订单状态转为REFUNDING；创建PENDING状态的退款记录。
     *
     * @param ownerId 宠物主人用户ID
     * @param orderId 订单ID
     * @param reason  退款原因
     * @return 创建的退款记录（状态：PENDING）
     */
    Refund createRefundByOrderId(Long ownerId, Long orderId, String reason);

    /**
     * 【创建退款申请】
     *
     * 业务作用：
     * 核心创建退款逻辑：校验可退款状态、防止重复申请、标记订单为REFUNDING、创建退款记录。
     *
     * 调用场景：
     * createRefundByOrderId内部调用，或直接通过订单号创建退款。
     *
     * 调用链：
     * createRefundByOrderId/Controller
     * ↓
     * createRefund(ownerId, orderNo, reason)
     * ↓
     * 查询订单 → 校验可退款状态 → 查重(已有PENDING/APPROVED) → 乐观锁更新订单REFUNDING → insert退款记录
     *
     * 业务规则：
     * 1. 可退款状态集合：PAID/CONFIRMED/DELIVERED/RECEIVED/IN_PROGRESS
     * 2. 不允许存在未完成的退款申请（PENDING或APPROVED）
     * 3. 保存退款前的订单状态(order_status_before_refund)，用于驳回时恢复
     *
     * 状态影响：
     * 订单：原状态 → REFUNDING；创建退款记录（PENDING）。
     *
     * 异常情况：
     * 订单不存在抛404；不可退款状态抛400；已有处理中的退款抛400。
     *
     * @param ownerId 宠物主人用户ID
     * @param orderNo 订单编号
     * @param reason  退款原因
     * @return 创建的退款记录（状态：PENDING）
     */
    Refund createRefund(Long ownerId, String orderNo, String reason);

    /**
     * 【审核通过退款申请】
     *
     * 业务作用：
     * 管理员审核通过退款的PENDING申请，标记为APPROVED等待执行退款。
     *
     * 调用场景：
     * 管理后台退款审核通过操作。
     *
     * 调用链：
     * 管理后台/Controller
     * ↓
     * approveRefund(refundId)
     * ↓
     * 查询退款 → 乐观锁更新状态APPROVED
     *
     * 业务规则：
     * 仅PENDING状态的退款可审核通过。
     *
     * 状态影响：
     * 退款状态：PENDING → APPROVED
     *
     * @param refundId 退款ID
     */
    void approveRefund(Long refundId);

    /**
     * 【完成退款（执行退款转账）】
     *
     * 业务作用：
     * 执行已审核通过的退款，从系统账户向宠物主人转账退款金额，同时回冲平台补贴。
     * 完成后订单状态转为REFUNDED。
     *
     * 调用场景：
     * 退款审核通过后，运营人员执行"完成退款"操作。
     *
     * 调用链：
     * 管理后台/Controller
     * ↓
     * completeRefund(refundId)
     * ↓
     * 查询退款 → 校验状态/订单 → accountingService.transfer(退款转账)
     * → accountingService.debit(回冲补贴) → 乐观锁更新退款COMPLETED → 乐观锁更新订单REFUNDED
     *
     * 业务规则：
     * 1. 仅APPROVED状态可完成退款
     * 2. 已完成的订单（COMPLETED）不支持退款
     * 3. 订单必须处于REFUNDING状态
     * 4. 全额退款 + 回冲平台补贴
     *
     * 状态影响：
     * 退款：APPROVED → COMPLETED；订单：REFUNDING → REFUNDED
     *
     * @param refundId 退款ID
     */
    void completeRefund(Long refundId);

    /**
     * 【驳回退款申请】
     *
     * 业务作用：
     * 管理员驳回退款的PENDING申请，驳回后订单状态恢复到退款前的状态。
     *
     * 调用场景：
     * 管理后台退款审核驳回操作。
     *
     * 调用链：
     * 管理后台/Controller
     * ↓
     * rejectRefund(refundId)
     * ↓
     * 查询退款 → 乐观锁更新退款REJECTED → 恢复订单为退款前状态
     *
     * 业务规则：
     * 1. 仅PENDING状态可驳回
     * 2. 驳回后自动恢复订单状态（从REFUNDING恢复到退款前状态）
     * 3. 恢复状态取 refund.order_status_before_refund
     *
     * 状态影响：
     * 退款：PENDING → REJECTED；订单：REFUNDING → 退款前状态
     *
     * @param refundId 退款ID
     */
    void rejectRefund(Long refundId);

    /**
     * 【退款实体转DTO】
     *
     * 业务作用：
     * 将退款实体映射为退款DTO。
     *
     * @param entity 退款实体，可为null
     * @return 退款DTO
     */
    RefundDTO toDTO(Refund entity);
}
