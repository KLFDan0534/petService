package com.pet.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.common.BusinessException;
import com.pet.common.PageRequestDTO;
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
import com.pet.order.pricing.PaymentPurpose;
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

    /**
     * 【构造函数：注入支付链路依赖】
     *
     * 业务作用：注入支付单、订单 Mapper，以及状态广播、账务、优惠券、会员权益、MQ 发送与订单服务。
     *
     * 调用场景：容器启动创建 PaymentServiceImpl Bean 时自动调用。
     *
     * 调用链：Spring IoC → PaymentServiceImpl(构造) → 各依赖字段赋值。
     *
     * 数据处理：仅做字段赋值。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：支付超时窗口 PAYMENT_TIMEOUT 取自 RabbitMQConfig 的 TTL 常量，与 MQ 延迟队列保持一致。
     */
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
        log.info("按订单号查询支付记录: {}", orderNo);
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
                        .eq(Payment::getPurpose_wsh, PaymentPurpose.ORDER)
                        .orderByDesc(Payment::getId_wsh)
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
        log.info("查询用户支付记录: {}", userId);
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
        log.info("为订单创建支付: {}, 支付方式: {}", orderId, method);
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
        log.info("为订单创建支付: {}, 支付方式: {}", orderNo, method);
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
                        .eq(Payment::getPurpose_wsh, PaymentPurpose.ORDER)
                        .eq(Payment::getStatus_wsh, "pending")
                        .orderByDesc(Payment::getId_wsh)
                        .last("LIMIT 1"));
        // 已有待支付单则复用，只更新支付方式
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
        payment.setPurpose_wsh(PaymentPurpose.ORDER);
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
        log.info("执行支付: {}", payNo);
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getPay_no_wsh, payNo).last("LIMIT 1"));
        if (payment == null) throw new BusinessException(404, "支付记录不存在");

        PetOrder order = orderMapper.selectById(payment.getOrder_id_wsh());
        if (order == null) throw new BusinessException(404, "订单不存在");
        // 归属校验必须排在「已支付幂等返回」之前：其他主人或本单 keeper 一律 403
        if (!userId.equals(order.getOwner_id_wsh())) throw new BusinessException(403, "无权支付此订单");

        // 先按用途分流，再走各自的状态校验：
        // 补费不受原订单 created_at+15 分钟限制，也不推进 pending→paid
        if (PaymentPurpose.NIGHT_OVERTIME.equals(payment.getPurpose_wsh())) {
            payNightOvertime(payment, order);
            return;
        }

        // 已支付直接返回，保证重复调用幂等
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
        // 并发下已被支付则按成功处理
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
            // 余额支付才从主人账户扣款
            if ("balance".equals(payment.getMethod_wsh())) {
                accountingService.debit(order.getOwner_id_wsh(), paymentAmount, "payment", order.getId_wsh(),
                        "payment", String.valueOf(payment.getId_wsh()), requestId + ":owner", "订单余额支付 - " + order.getOrder_no_wsh());
            }
            accountingService.credit(accountingService.systemUserId(), paymentAmount, "payment", order.getId_wsh(),
                    "payment", String.valueOf(payment.getId_wsh()), requestId + ":system", "订单支付进入系统暂存 - " + order.getOrder_no_wsh());
        }

        // 平台补贴单独入账，与用户实付分开
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
        // 订单状态被并发改动时终止本次支付
        if (orderUpdated == 0) {
            throw new BusinessException(400, "订单状态已变化，无法支付");
        }
        order.setStatus_wsh(OrderStatus.PAID);
        couponService.markUsedForOrder(order.getId_wsh(), order.getOrder_no_wsh());
        membershipBenefitService.markUsedForOrder(order.getId_wsh(), order.getOrder_no_wsh());
        orderStatusBroadcaster.broadcast(order);
        if (order.getKeeper_id_wsh() == null) {
            // 无负责人的订单进入待分配池：实时通知同商家在职看护者，避免只能靠手动刷新
            orderStatusBroadcaster.broadcastPoolChange(order, "pool-new");
        }
        scheduleAcceptTimeoutCheck(order);
    }

    /**
     * 【晚接补费支付】
     *
     * 业务作用：
     * 主人确认余额支付补费单：扣款、系统入账、支付成功、累计金额与完成订单在同一个事务内完成。
     *
     * 与预付支付的区别（都必须遵守）：
     * - 不调用 ensureOrderPaymentNotExpired：补费不受下单后 15 分钟限制（三天前的订单也能补费）
     * - 不推进订单 pending→paid、不核销券/会员、不再记平台补贴、不安排接单超时
     * - 只允许余额支付（keeper 不能凭码代扣）
     * - 加锁顺序：订单行 → 支付单 → 钱包
     *
     * 幂等与失败：
     * - 支付单已成功：直接返回，不重复扣款/累计/结算
     * - 余额不足等任何异常：整次事务回滚，但先前已提交的接走时间 T 与补费单保留，可原单重试
     *
     * @param payment 补费支付单
     * @param order   关联订单（调用方已校验主人归属）
     */
    private void payNightOvertime(Payment payment, PetOrder order) {
        // 1. 订单行锁：与接走确认/退款保持同一加锁顺序
        PetOrder lockedOrder = orderMapper.selectByIdForUpdate(order.getId_wsh());
        if (lockedOrder == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if ("success".equals(payment.getStatus_wsh())) {
            // 幂等：若上次事务已提交，订单应已完成；若订单仍在服务中则补一次收尾
            if (OrderStatus.IN_PROGRESS.equals(lockedOrder.getStatus_wsh())) {
                orderService.applyOvertimePayment(lockedOrder.getId_wsh(), payment.getId_wsh());
            }
            return;
        }
        if (!"pending".equals(payment.getStatus_wsh())) {
            throw new BusinessException(400, "当前支付状态不可支付");
        }
        if (!OrderStatus.IN_PROGRESS.equals(lockedOrder.getStatus_wsh())) {
            throw new BusinessException(400, "当前订单状态不可支付补费");
        }
        if (!payment.getId_wsh().equals(lockedOrder.getOvertime_payment_id_wsh())) {
            throw new BusinessException(400, "补费支付单与订单不匹配");
        }
        BigDecimal amount = defaultMoney(payment.getAmount_wsh());
        if (amount.compareTo(defaultMoney(lockedOrder.getOvertime_amount_wsh())) != 0) {
            throw new BusinessException(400, "补费金额与订单不一致，请刷新后重试");
        }
        // 补费只允许余额支付
        if (!METHOD_BALANCE.equals(payment.getMethod_wsh())) {
            throw new BusinessException(400, "晚接补费仅支持余额支付");
        }
        ensureUserExecutableMethod(payment.getMethod_wsh());

        // 2. 支付单条件更新：并发下只有一方能把 pending 改成 success
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

        // 3. 钱包：扣款 + 系统入账（requestId 唯一，重复调用不会二次记账）
        String requestId = "payment:" + payment.getId_wsh();
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            accountingService.debit(lockedOrder.getOwner_id_wsh(), amount, "payment",
                    lockedOrder.getId_wsh(), "payment", String.valueOf(payment.getId_wsh()),
                    requestId + ":owner", "晚接补费 - " + lockedOrder.getOrder_no_wsh());
            accountingService.credit(accountingService.systemUserId(), amount, "payment",
                    lockedOrder.getId_wsh(), "payment", String.valueOf(payment.getId_wsh()),
                    requestId + ":system", "晚接补费进入系统暂存 - " + lockedOrder.getOrder_no_wsh());
        }

        // 4. 累计金额并完成（同事务；失败则整体回滚，T 与补费单不受影响）
        orderService.applyOvertimePayment(lockedOrder.getId_wsh(), payment.getId_wsh());
    }

    /**
     * 【管理员分页查询支付记录（实现）】
     *
     * 业务作用：
     * 分页查询支付记录，keyword 非空时按订单号/支付编号模糊搜索，按创建时间倒序排列。
     *
     * 调用链：
     * PaymentService.pageAll()
     * ↓
     * paymentMapper.selectPage(Page, LambdaQueryWrapper)
     *
     * 状态影响：
     * 只读操作。
     *
     * @param pageParam 分页参数
     * @param keyword   搜索关键字（可为null，用于按订单号/支付编号模糊搜索）
     * @return 分页的支付记录
     */
    @Override
    public IPage<Payment> pageAll(PageRequestDTO pageParam, String keyword) {
        log.info("分页查询支付记录, page: {}, size: {}, keyword: {}", pageParam.getPage(), pageParam.getSize(), keyword);
        Page<Payment> page = new Page<>(pageParam.getPage(), pageParam.getSize());
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<Payment>()
                .orderByDesc(Payment::getCreated_at_wsh);
        // 关键字同时匹配订单号与支付编号
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Payment::getOrder_no_wsh, kw)
                    .or()
                    .like(Payment::getPay_no_wsh, kw));
        }
        return paymentMapper.selectPage(page, wrapper);
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
        log.info("将 Payment 实体转换为 DTO");
        if (entity == null) return null;
        PaymentDTO dto = new PaymentDTO();
        dto.setId_wsh(entity.getId_wsh());
        dto.setOrder_id_wsh(entity.getOrder_id_wsh());
        dto.setOrder_no_wsh(entity.getOrder_no_wsh());
        dto.setPay_no_wsh(entity.getPay_no_wsh());
        dto.setAmount_wsh(entity.getAmount_wsh());
        dto.setMethod_wsh(entity.getMethod_wsh());
        dto.setPurpose_wsh(entity.getPurpose_wsh());
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
        // 兼容旧客户端的 online 参数
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

    /**
     * 【金额空值兜底】
     *
     * 业务作用：把可空金额转为可参与运算的 BigDecimal，null 时返回 0。
     *
     * 调用场景：pay 方法中读取支付金额与平台补贴金额。
     *
     * 调用链：PaymentServiceImpl.pay → defaultMoney → BigDecimal.ZERO。
     *
     * 数据处理：null 返回 BigDecimal.ZERO，否则原样返回。
     *
     * 业务规则：不做小数位归一，也不校验负数。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：与 CouponServiceImpl.moneyOrZero 不同，此处不 setScale(2) 也不拒绝负数，仅用于避免空指针。
     */
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
        // 未超时直接放行
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
                log.warn("发送订单接单超时消息失败, 订单编号: {}", orderNo, e);
            }
        };
        // 有事务时延后到提交后再发消息
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
