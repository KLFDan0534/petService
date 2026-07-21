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

    @Override
    public Payment getByOrderNo(Long ownerId, String orderNo) {
        log.info("调用 getByOrderNo()");
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

    @Override
    public List<Payment> listByUser(Long userId) {
        log.info("调用 listByUser()");
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

    @Transactional
    @Override
    public Payment createPaymentByOrderId(Long ownerId, Long orderId, String method) {
        log.info("调用 createPaymentByOrderId()");
        PetOrder order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException(404, "订单不存在");
        if (!ownerId.equals(order.getOwner_id_wsh())) throw new BusinessException(403, "无权操作此订单");
        return createPayment(ownerId, order.getOrder_no_wsh(), method);
    }

    @Transactional
    @Override
    public Payment createPayment(Long ownerId, String orderNo, String method) {
        log.info("调用 createPayment()");
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

    @Transactional
    @Override
    public void pay(Long userId, String payNo) {
        log.info("调用 pay()");
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

    @Override
    public PaymentDTO toDTO(Payment entity) {
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

    private void ensureOrderPaymentNotExpired(PetOrder order) {
        LocalDateTime createdAt = order.getCreated_at_wsh();
        if (createdAt == null || createdAt.plus(PAYMENT_TIMEOUT).isAfter(LocalDateTime.now())) {
            return;
        }
        boolean cancelled = orderService.cancelPendingOrderIfPaymentTimeout(order.getOrder_no_wsh());
        String message = cancelled ? "订单已超过15分钟支付时限，已自动取消" : "订单已超过15分钟支付时限，请刷新后重试";
        throw new BusinessException(400, message);
    }

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
