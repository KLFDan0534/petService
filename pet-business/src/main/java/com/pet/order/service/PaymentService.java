package com.pet.order.service;

import com.pet.order.dto.PaymentDTO;
import com.pet.order.entity.Payment;
import java.util.List;

/**
 * Service interface for payment operations on pet boarding orders.
 * <p>
 * Payment is the mechanism by which pet owners fund their orders. The payment flow:
 * <ol>
 *   <li>Create a payment record ({@link #createPayment} / {@link #createPaymentByOrderId})</li>
 *   <li>Execute the payment ({@link #pay}) which transitions the order from PENDING to PAID</li>
 * </ol>
 * Supported payment methods: balance, wechat, alipay.
 */
public interface PaymentService {

    /**
     * 【根据订单号查询支付记录】
     *
     * 业务作用：
     * 根据订单编号和主人ID查询支付记录，用于支付状态查询。
     *
     * 调用场景：
     * 用户查看订单支付详情时调用。
     *
     * 调用链：
     * Controller
     * ↓
     * getByOrderNo(ownerId, orderNo)
     * ↓
     * orderMapper查询校验归属 → paymentMapper查询支付记录
     *
     * 数据处理：
     * 先校验订单存在性和归属，再查询支付记录。
     *
     * 状态影响：
     * 只读操作。
     *
     * @param ownerId 宠物主人用户ID
     * @param orderNo 订单编号
     * @return 支付记录，不存在时返回null
     */
    Payment getByOrderNo(Long ownerId, String orderNo);

    /**
     * 【查询用户的所有支付记录】
     *
     * 业务作用：
     * 查询指定用户的所有订单对应的支付记录，用于用户端"我的支付"列表。
     *
     * 调用场景：
     * 用户查看自己的所有支付记录。
     *
     * 调用链：
     * Controller
     * ↓
     * listByUser(userId)
     * ↓
     * orderMapper查询用户订单 → paymentMapper查询支付记录
     *
     * 数据处理：
     * 先查出用户的所有订单ID，再根据订单ID批量查询支付记录。
     *
     * 状态影响：
     * 只读操作。
     *
     * @param userId 用户ID
     * @return 支付记录列表
     */
    List<Payment> listByUser(Long userId);

    /**
     * 【根据订单ID创建支付记录】
     *
     * 业务作用：
     * 根据订单ID创建支付记录，校验归属后委托createPayment处理。支持支付方式：余额、微信、支付宝。
     *
     * 调用场景：
     * 用户选择支付方式后点击"去支付"。
     *
     * 调用链：
     * Controller
     * ↓
     * createPaymentByOrderId(ownerId, orderId, method)
     * ↓
     * orderMapper校验 → createPayment(ownerId, orderNo, method)
     *
     * 数据处理：
     * 先查订单校验归属，取订单号委托createPayment。
     *
     * 业务规则：
     * 1. 校验订单归属
     * 2. 如果已有待支付记录则返回（更新支付方式）
     * 3. 订单必须为PENDING状态且未超时
     *
     * 状态影响：
     * 创建支付记录（状态pending），不修改订单状态。
     *
     * @param ownerId 宠物主人用户ID
     * @param orderId 订单ID
     * @param method  支付方式（balance/wechat/alipay）
     * @return 创建的支付记录
     */
    Payment createPaymentByOrderId(Long ownerId, Long orderId, String method);

    /**
     * 【根据订单号创建支付记录】
     *
     * 业务作用：
     * 根据订单号创建支付记录，生成唯一支付编号（PAY前缀），支持重复创建时复用已有待支付记录。
     *
     * 调用场景：
     * createPaymentByOrderId内部调用，或直接通过订单号创建支付。
     *
     * 调用链：
     * createPaymentByOrderId/Controller
     * ↓
     * createPayment(ownerId, orderNo, method)
     * ↓
     * 校验订单状态/超时 → 查已有待支付记录 → 存在则更新方式/不存在则新建 → insert支付记录
     *
     * 业务规则：
     * 1. 订单必须PENDING状态且未支付
     * 2. 支付窗口不能超时（默认15分钟）
     * 3. 存在待支付记录时复用（仅更新支付方式）
     * 4. 支付编号格式：PAY + UUID(16位大写)
     *
     * 状态影响：
     * 创建支付记录。
     *
     * 异常情况：
     * 订单已支付抛400；状态不可支付抛400；超时自动取消并抛400。
     *
     * @param ownerId 宠物主人用户ID
     * @param orderNo 订单编号
     * @param method  支付方式（balance/wechat/alipay）
     * @return 创建的支付记录
     */
    Payment createPayment(Long ownerId, String orderNo, String method);

    /**
     * 【执行支付】
     *
     * 业务作用：
     * 执行付款操作，将支付记录从pending转为success，订单从PENDING转为PAID。
     * 同时处理账务记账（余额扣款、系统暂存、平台补贴入账）和后续流程（券使用、广播、超时检查）。
     *
     * 调用场景：
     * 用户确认支付时调用。
     *
     * 调用链：
     * Controller
     * ↓
     * pay(userId, payNo)
     * ↓
     * 查询支付记录 → 查询订单 → 校验状态/归属/超时/支付方式
     * → 乐观锁更新支付success → 余额扣款(debit) → 系统入账(credit) → 补贴入账
     * → 乐观锁更新订单PAID → 标记券已使用 → 标记会员已使用 → SSE广播 → 发送接单超时检查消息
     *
     * 数据处理：
     * 1. 余额支付：从主人账户扣款
     * 2. 系统入账：支付金额暂存系统账户
     * 3. 优惠券补贴：平台补贴部分入账系统账户
     *
     * 业务规则：
     * 1. 仅支持余额(balance)方式直接执行，微信/支付宝需外部确认
     * 2. 已成功的支付幂等返回
     * 3. 支付窗口超时自动取消订单
     *
     * 状态影响：
     * 支付记录：pending → success；订单：PENDING → PAID
     *
     * 异常情况：
     * 支付记录不存在抛404；非本人抛403；状态不可支付/超时抛400。
     *
     * @param userId 用户ID（必须为订单主人）
     * @param payNo  支付编号（PAY前缀）
     */
    void pay(Long userId, String payNo);

    /**
     * 【支付实体转DTO】
     *
     * 业务作用：
     * 将Payment实体转换为PaymentDTO，手动映射字段。
     *
     * @param entity 支付实体（可为null）
     * @return 支付DTO
     */
    PaymentDTO toDTO(Payment entity);
}
