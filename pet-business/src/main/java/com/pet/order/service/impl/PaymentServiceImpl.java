package com.pet.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.pet.common.BusinessException;
import com.pet.common.OrderStatus;
import com.pet.config.RabbitMQConfig;
import com.pet.finance.service.AccountingService;
import com.pet.marketing.service.CouponService;
import com.pet.membership.service.MembershipBenefitService;
import com.pet.mq.MessageSender;
import com.pet.order.dto.PaymentDTO;
import com.pet.order.entity.Payment;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.order.mapper.PaymentMapper;
import com.pet.order.service.OrderService;
import com.pet.order.service.OrderStatusBroadcaster;
import com.pet.order.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of {@link PaymentService} for payment processing.
 * <p>
 * Handles payment record creation and execution. Supported payment methods:
 * balance (internal account), wechat, and alipay. Payment execution transitions
 * the order from PENDING to PAID, performs accounting entries (debiting the
 * owner or crediting the system), and schedules accept-timeout monitoring.
 * <p>
 * <b>Payment timeout:</b> Orders must be paid within a configured window
 * (default 15 minutes). Expired payments trigger automatic order cancellation.
 */
@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {
    private static final String METHOD_BALANCE = "balance";
    private static final String METHOD_WECHAT = "wechat";
    private static final String METHOD_ALIPAY = "alipay";
    private static final List<String> SUPPORTED_METHODS = List.of(METHOD_BALANCE, METHOD_WECHAT, METHOD_ALIPAY);

    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;
    private final OrderStatusBroadcaster orderStatusBroadcaster;
    private final AccountingService accountingService;
    private final CouponService couponService;
    private final MembershipBenefitService membershipBenefitService;
    private final MessageSender messageSender;
    private final OrderService orderService;
    private static final Duration PAYMENT_TIMEOUT = Duration.ofMillis(RabbitMQConfig.ORDER_PAYMENT_TIMEOUT_TTL_MS);

    public PaymentServiceImpl(PaymentMapper paymentMapper,
                              OrderMapper orderMapper,
                              OrderStatusBroadcaster orderStatusBroadcaster,
                              AccountingService accountingService,
                              CouponService couponService,
                              MembershipBenefitService membershipBenefitService,
                              MessageSender messageSender,
                              OrderService orderService) {
        this.paymentMapper = paymentMapper;
        this.orderMapper = orderMapper;
        this.orderStatusBroadcaster = orderStatusBroadcaster;
        this.accountingService = accountingService;
        this.couponService = couponService;
        this.membershipBenefitService = membershipBenefitService;
        this.messageSender = messageSender;
        this.orderService = orderService;
    }

    /**
     * 【查询支付记录（实现）】
     *
     * 业务作用：
     * 按订单号和主人ID查询支付记录，先校验订单归属再查支付。
     *
     * @param ownerId 主人ID
     * @param orderNo 订单号
     * @return 支付记录
     */
    @Override
    public Payment getByOrderNo(Long ownerId, String orderNo) {
        log.info("Query payment by orderNo: {}", orderNo);
        PetOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<PetOrder>()
                        .eq(PetOrder::getOrder_no_wsh, orderNo)
                        .eq(PetOrder::getOwner_id_wsh, ownerId)
                        .last("LIMIT 1"));
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        return paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getOrder_no_wsh, orderNo)
                        .last("LIMIT 1"));
    }

    /**
     * 【查询用户支付记录列表（实现）】
     *
     * 业务作用：
     * 通过用户订单ID批量查询关联支付记录，按创建时间降序排列。
     *
     * 调用链：
     * PaymentService.listByUser()
     * ↓
     * orderMapper查询用户所有订单ID → paymentMapper批量查询支付记录
     *
     * 状态影响：
     * 只读操作。
     *
     * @param userId 用户ID
     * @return 支付记录列表
     */
    @Override
    public List<Payment> listByUser(Long userId) {
        log.info("Query payments for user: {}", userId);
        List<PetOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<PetOrder>()
                        .eq(PetOrder::getOwner_id_wsh, userId)
                        .select(PetOrder::getId_wsh));
        if (orders.isEmpty()) return List.of();
        List<Long> orderIds = orders.stream().map(PetOrder::getId_wsh).collect(Collectors.toList());
        return paymentMapper.selectList(
                new LambdaQueryWrapper<Payment>()
                        .in(Payment::getOrder_id_wsh, orderIds)
                        .orderByDesc(Payment::getCreated_at_wsh));
    }

    /**
     * 【根据订单ID创建支付记录（实现）】
     *
     * 业务作用：
     * 校验订单存在性和归属后，取订单号委托createPayment处理。
     *
     * 调用链：
     * PaymentService.createPaymentByOrderId()
     * ↓
     * orderMapper查询校验 → createPayment(ownerId, orderNo, method)
     *
     * @param ownerId 主人ID
     * @param orderId 订单ID
     * @param method  支付方式
     * @return 支付记录
     */
    @Transactional
    @Override
    public Payment createPaymentByOrderId(Long ownerId, Long orderId, String method) {
        log.info("Create payment for order: {}, method: {}", orderId, method);
        PetOrder order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException(404, "订单不存在");
        if (!ownerId.equals(order.getOwner_id_wsh())) throw new BusinessException(403, "无权操作此订单");
        return createPayment(ownerId, order.getOrder_no_wsh(), method);
    }

    /**
     * 【创建支付记录（实现）】
     *
     * 业务作用：
     * 核心创建支付逻辑：校验状态/超时 → 复用已有待支付记录 → 生成新支付记录。
     *
     * 调用链：
     * PaymentService.createPayment()
     * ↓
     * 查询订单 → 校验(已支付/状态/超时) → 查已有待支付记录
     * → 存在则更新支付方式 → 不存在则生成PAY+UUID编号 → insert
     *
     * 业务规则：
     * 1. 订单必须PENDING且未支付
     * 2. 支付超时检查：ensureOrderPaymentNotExpired()
     * 3. 支付方式标准化：normalizeMethod()（online→wechat，null→balance）
     *
     * @param ownerId 主人ID
     * @param orderNo 订单号
     * @param method  支付方式
     * @return 支付记录
     */
    @Transactional
    @Override
    public Payment createPayment(Long ownerId, String orderNo, String method) {
        log.info("Create payment for order: {}, method: {}", orderNo, method);
        PetOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<PetOrder>()
                        .eq(PetOrder::getOrder_no_wsh, orderNo)
                        .eq(PetOrder::getOwner_id_wsh, ownerId));
        if (order == null) throw new BusinessException(404, "订单不存在");
        if (OrderStatus.PAID.equals(order.getStatus_wsh())) throw new BusinessException(400, "订单已支付");
        if (!OrderStatus.PENDING.equals(order.getStatus_wsh())) throw new BusinessException(400, "当前订单状态不可支付");
        ensureOrderPaymentNotExpired(order);

        Payment existing = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getOrder_no_wsh, orderNo)
                        .eq(Payment::getStatus_wsh, "pending")
                        .last("LIMIT 1"));
        if (existing != null) {
            String normalized = normalizeMethod(method);
            if (!normalized.equals(existing.getMethod_wsh())) {
                existing.setMethod_wsh(normalized);
                paymentMapper.updateById(existing);
            }
            return existing;
        }

        Payment payment = new Payment();
        payment.setOrder_id_wsh(order.getId_wsh());
        payment.setOrder_no_wsh(orderNo);
        payment.setPay_no_wsh("PAY" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        payment.setAmount_wsh(order.getFinal_amount_wsh());
        payment.setMethod_wsh(normalizeMethod(method));
        payment.setStatus_wsh("pending");
        paymentMapper.insert(payment);
        return payment;
    }

    /**
     * 【执行支付（实现）】
     *
     * 业务作用：
     * 支付执行核心逻辑：支付记录success→订单PAID→账务处理→后续流程触发。
     *
     * 调用链：
     * PaymentService.pay()
     * ↓
     * 查询支付记录 → 查询订单 → 校验(归属/状态/超时/支付方式)
     * → 乐观锁更新支付success → [余额支付]主人扣款 → 系统入账 → 平台补贴入账
     * → 乐观锁更新订单PAID → 标记券已使用 → 标记会员已使用 → SSE广播 → scheduleAcceptTimeoutCheck()
     *
     * 账务处理：
     * 1. 余额支付：accountingService.debit(主人, 金额, "payment")
     * 2. 支付入账：accountingService.credit(系统用户, 金额, "payment")
     * 3. 补贴入账：accountingService.credit(系统用户, 补贴, "coupon_subsidy")
     *
     * 事务一致性：
     * 支付记录乐观锁失败时重新查询，若已成功则幂等返回。
     *
     * @param userId 用户ID
     * @param payNo  支付编号
     */
    @Transactional
    @Override
    public void pay(Long userId, String payNo) {
        log.info("Execute payment: {}", payNo);
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getPay_no_wsh, payNo).last("LIMIT 1"));
        if (payment == null) throw new BusinessException(404, "支付记录不存在");

        PetOrder order = orderMapper.selectById(payment.getOrder_id_wsh());
        if (order == null) throw new BusinessException(404, "订单不存在");
        if (!userId.equals(order.getOwner_id_wsh())) throw new BusinessException(403, "无权支付此订单");

        if ("success".equals(payment.getStatus_wsh())) {
            return;
        }
        if (!"pending".equals(payment.getStatus_wsh())) {
            throw new BusinessException(400, "当前支付状态不可支付");
        }
        if (!OrderStatus.PENDING.equals(order.getStatus_wsh())) {
            throw new BusinessException(400, "当前订单状态不可支付");
        }
        ensureOrderPaymentNotExpired(order);
        ensureUserExecutableMethod(payment.getMethod_wsh());

        payment.setStatus_wsh("success");
        payment.setPaid_at_wsh(LocalDateTime.now());
        int paid = paymentMapper.update(payment, new LambdaUpdateWrapper<Payment>()
                .eq(Payment::getId_wsh, payment.getId_wsh())
                .eq(Payment::getStatus_wsh, "pending"));
        if (paid == 0) {
            Payment latest = paymentMapper.selectById(payment.getId_wsh());
            if (latest != null && "success".equals(latest.getStatus_wsh())) {
                return;
            }
            throw new BusinessException(400, "当前支付状态不可支付");
        }

        String requestId = "payment:" + payment.getId_wsh();
        BigDecimal paymentAmount = defaultMoney(payment.getAmount_wsh());
        if (paymentAmount.compareTo(BigDecimal.ZERO) > 0) {
            if ("balance".equals(payment.getMethod_wsh())) {
                accountingService.debit(order.getOwner_id_wsh(), paymentAmount, "payment", order.getId_wsh(),
                        "payment", String.valueOf(payment.getId_wsh()), requestId + ":owner", "订单余额支付 - " + order.getOrder_no_wsh());
            }
            accountingService.credit(accountingService.systemUserId(), paymentAmount, "payment", order.getId_wsh(),
                    "payment", String.valueOf(payment.getId_wsh()), requestId + ":system", "订单支付进入系统暂存 - " + order.getOrder_no_wsh());
        }

        BigDecimal subsidy = defaultMoney(order.getPlatform_subsidy_wsh());
        if (subsidy.compareTo(BigDecimal.ZERO) > 0) {
            accountingService.credit(accountingService.systemUserId(), subsidy, "coupon_subsidy", order.getId_wsh(),
                    "coupon", String.valueOf(order.getCoupon_id_wsh()), requestId + ":coupon-subsidy",
                    "平台优惠券补贴进入系统暂存 - " + order.getOrder_no_wsh());
        }

        PetOrder update = new PetOrder();
        update.setStatus_wsh(OrderStatus.PAID);
        int orderUpdated = orderMapper.update(update, new LambdaUpdateWrapper<PetOrder>()
                .eq(PetOrder::getId_wsh, order.getId_wsh())
                .eq(PetOrder::getStatus_wsh, OrderStatus.PENDING));
        if (orderUpdated == 0) {
            throw new BusinessException(400, "订单状态已变化，无法支付");
        }
        order.setStatus_wsh(OrderStatus.PAID);
        couponService.markUsedForOrder(order.getId_wsh(), order.getOrder_no_wsh());
        membershipBenefitService.markUsedForOrder(order.getId_wsh(), order.getOrder_no_wsh());
        orderStatusBroadcaster.broadcast(order);
        scheduleAcceptTimeoutCheck(order);
    }

    /**
     * 【支付实体转DTO（实现）】
     *
     * 业务作用：
     * 手动映射Payment实体字段到PaymentDTO。
     *
     * @param entity 支付实体，可为null
     * @return 支付DTO
     */
    @Override
    public PaymentDTO toDTO(Payment entity) {
        log.info("Convert Payment entity to DTO");
        if (entity == null) return null;
        PaymentDTO dto = new PaymentDTO();
        dto.setId_wsh(entity.getId_wsh());
        dto.setOrder_id_wsh(entity.getOrder_id_wsh());
        dto.setOrder_no_wsh(entity.getOrder_no_wsh());
        dto.setPay_no_wsh(entity.getPay_no_wsh());
        dto.setAmount_wsh(entity.getAmount_wsh());
        dto.setMethod_wsh(entity.getMethod_wsh());
        dto.setStatus_wsh(entity.getStatus_wsh());
        dto.setPaid_at_wsh(entity.getPaid_at_wsh());
        dto.setCreated_at_wsh(entity.getCreated_at_wsh());
        return dto;
    }

    /**
     * Normalizes a payment method string to a supported internal value.
     * Defaults to "balance" if null/blank. Maps "online" to "wechat".
     * "mock" payments are no longer supported.
     *
     * @param method the raw payment method string
     * @return the normalized method (balance, wechat, or alipay)
     * @throws BusinessException if the method is unsupported or mock (deprecated)
     */
    private String normalizeMethod(String method) {
        if (method == null || method.isBlank()) return METHOD_BALANCE;
        String value = method.trim().toLowerCase();
        if ("online".equals(value)) return METHOD_WECHAT;
        if ("mock".equals(value)) {
            throw new BusinessException(400, "模拟支付已停用");
        }
        if (SUPPORTED_METHODS.contains(value)) return value;
        throw new BusinessException(400, "不支持的支付方式");
    }

    /**
     * Validates that the payment method can be executed by the user directly.
     * Currently only "balance" is executable; wechat/alipay require external
     * payment platform confirmation and cannot be executed via this API.
     *
     * @param method the normalized payment method
     * @throws BusinessException if the method requires external confirmation or is mock
     */
    private void ensureUserExecutableMethod(String method) {
        if (METHOD_BALANCE.equals(method)) {
            return;
        }
        if ("mock".equals(method)) {
            throw new BusinessException(400, "模拟支付已停用");
        }
        throw new BusinessException(400, "该支付方式需等待支付平台确认");
    }

    private BigDecimal defaultMoney(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    /**
     * Checks whether the order's payment window has expired. If expired, attempts
     * to cancel the order via {@link OrderService#cancelPendingOrderIfPaymentTimeout}
     * and throws an exception with an appropriate message.
     *
     * @param order the order to check
     * @throws BusinessException if the payment window has expired
     */
    private void ensureOrderPaymentNotExpired(PetOrder order) {
        LocalDateTime createdAt = order.getCreated_at_wsh();
        if (createdAt == null || createdAt.plus(PAYMENT_TIMEOUT).isAfter(LocalDateTime.now())) {
            return;
        }
        boolean cancelled = orderService.cancelPendingOrderIfPaymentTimeout(order.getOrder_no_wsh());
        String message = cancelled ? "订单已超过15分钟支付时限，已自动取消" : "订单已超过15分钟支付时限，请刷新后重试";
        throw new BusinessException(400, message);
    }

    /**
     * Sends a delayed message to check the accept timeout after payment succeeds.
     * Uses {@link TransactionSynchronization#afterCommit()} to schedule the check
     * only after the current transaction commits. If no transaction is active,
     * sends immediately.
     *
     * @param order the newly paid order whose accept timeout should be monitored
     */
    private void scheduleAcceptTimeoutCheck(PetOrder order) {
        String orderNo = order.getOrder_no_wsh();
        if (orderNo == null || orderNo.isBlank()) {
            return;
        }
        Runnable task = () -> {
            try {
                messageSender.sendOrderAcceptTimeout(orderNo);
            } catch (Exception e) {
                log.warn("Failed to send order accept timeout message, orderNo: {}", orderNo, e);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }
}
