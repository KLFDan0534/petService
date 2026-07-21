package com.pet.membership.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.common.BusinessException;
import com.pet.finance.service.AccountingService;
import com.pet.membership.dto.MembershipOrderCreateRequestDTO;
import com.pet.membership.dto.MembershipOrderDTO;
import com.pet.membership.entity.MemberPlan;
import com.pet.membership.entity.MembershipEvent;
import com.pet.membership.entity.MembershipOrder;
import com.pet.membership.entity.UserMembership;
import com.pet.membership.mapper.MemberPlanMapper;
import com.pet.membership.mapper.MembershipEventMapper;
import com.pet.membership.mapper.MembershipOrderMapper;
import com.pet.membership.mapper.UserMembershipMapper;
import com.pet.membership.service.MembershipOrderService;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class MembershipOrderServiceImpl implements MembershipOrderService {
    private static final int PLAN_ENABLED = 1;
    private static final String METHOD_BALANCE = "balance";
    private static final String METHOD_WECHAT = "wechat";
    private static final String METHOD_ALIPAY = "alipay";
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_PAID = "paid";
    private static final String STATUS_CANCELLED = "cancelled";
    private static final List<String> SUPPORTED_PAY_METHODS = List.of(METHOD_BALANCE, METHOD_WECHAT, METHOD_ALIPAY);
    private static final int ACTIVATION_LOCK_SHARDS = 64;

    private final MembershipOrderMapper membershipOrderMapper;
    private final MemberPlanMapper memberPlanMapper;
    private final UserMembershipMapper userMembershipMapper;
    private final MembershipEventMapper membershipEventMapper;
    private final AccountingService accountingService;
    private final ObjectMapper objectMapper;
    private final Object[] activationLocks = createLockShards();

    public MembershipOrderServiceImpl(MembershipOrderMapper membershipOrderMapper,
                                      MemberPlanMapper memberPlanMapper,
                                      UserMembershipMapper userMembershipMapper,
                                      MembershipEventMapper membershipEventMapper,
                                      AccountingService accountingService,
                                      ObjectMapper objectMapper) {
        this.membershipOrderMapper = membershipOrderMapper;
        this.memberPlanMapper = memberPlanMapper;
        this.userMembershipMapper = userMembershipMapper;
        this.membershipEventMapper = membershipEventMapper;
        this.accountingService = accountingService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @Override
    public MembershipOrderDTO createOrder(Long userId, MembershipOrderCreateRequestDTO request) {
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        if (request == null) {
            throw new BusinessException(400, "请求体不能为空");
        }
        String requestId = normalizeRequestId(request.getRequest_id_wsh());
        MembershipOrder existing = findByRequestId(requestId);
        if (existing != null) {
            if (!Objects.equals(existing.getUser_id_wsh(), userId)) {
                throw new BusinessException(403, "无权访问该会员订单");
            }
            return toDTO(existing);
        }

        String payMethod = normalizePayMethod(request.getPay_method_wsh());
        MemberPlan plan = requireUsablePlan(request.getPlan_id_wsh(), request.getPlan_code_wsh());
        LocalDateTime startAt = resolveStartAt(userId);
        LocalDateTime endAt = startAt.plusDays(plan.getDuration_days_wsh());

        MembershipOrder order = new MembershipOrder();
        order.setOrder_no_wsh(generateOrderNo());
        order.setUser_id_wsh(userId);
        order.setPlan_id_wsh(plan.getId_wsh());
        order.setPlan_code_wsh(plan.getCode_wsh());
        order.setAmount_wsh(money(plan.getPrice_wsh()));
        order.setPay_method_wsh(payMethod);
        order.setStatus_wsh(STATUS_PENDING);
        order.setMembership_start_at_wsh(startAt);
        order.setMembership_end_at_wsh(endAt);
        order.setRequest_id_wsh(requestId);
        order.setPlan_snapshot_wsh(toPlanSnapshot(plan));
        order.setRemark_wsh(trimToNull(request.getRemark_wsh()));
        try {
            membershipOrderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            MembershipOrder duplicate = findByRequestId(requestId);
            if (duplicate != null && Objects.equals(duplicate.getUser_id_wsh(), userId)) {
                return toDTO(duplicate);
            }
            throw new BusinessException(400, "会员订单请求已被占用");
        }
        createEvent(order, "order_created", "创建会员订单");
        return toDTO(order);
    }

    @Override
    public List<MembershipOrderDTO> listMyOrders(Long userId) {
        return membershipOrderMapper.selectList(new LambdaQueryWrapper<MembershipOrder>()
                        .eq(MembershipOrder::getUser_id_wsh, userId)
                        .orderByDesc(MembershipOrder::getCreated_at_wsh)
                        .orderByDesc(MembershipOrder::getId_wsh))
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public List<MembershipOrderDTO> listOrdersForAdmin(String status) {
        String normalizedStatus = normalizeStatusOrNull(status);
        return membershipOrderMapper.selectList(new LambdaQueryWrapper<MembershipOrder>()
                        .eq(normalizedStatus != null, MembershipOrder::getStatus_wsh, normalizedStatus)
                        .orderByDesc(MembershipOrder::getCreated_at_wsh)
                        .orderByDesc(MembershipOrder::getId_wsh))
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public MembershipOrderDTO getMyOrder(Long userId, String orderNo) {
        MembershipOrder order = requireOrderByNo(orderNo);
        if (!Objects.equals(order.getUser_id_wsh(), userId)) {
            throw new BusinessException(403, "无权访问该会员订单");
        }
        return toDTO(order);
    }

    @Transactional
    @Override
    public MembershipOrderDTO cancelPendingOrder(Long userId, String orderNo) {
        MembershipOrder order = requireOrderByNo(orderNo);
        if (!Objects.equals(order.getUser_id_wsh(), userId)) {
            throw new BusinessException(403, "无权取消该会员订单");
        }
        if (!STATUS_PENDING.equals(order.getStatus_wsh())) {
            throw new BusinessException(400, "当前会员订单状态不可取消");
        }
        MembershipOrder update = new MembershipOrder();
        update.setStatus_wsh(STATUS_CANCELLED);
        int updated = membershipOrderMapper.update(update, new LambdaUpdateWrapper<MembershipOrder>()
                .eq(MembershipOrder::getId_wsh, order.getId_wsh())
                .eq(MembershipOrder::getStatus_wsh, STATUS_PENDING));
        if (updated == 0) {
            throw new BusinessException(400, "会员订单状态已变化，请刷新后重试");
        }
        order.setStatus_wsh(STATUS_CANCELLED);
        createEvent(order, "order_cancelled", "取消待支付会员订单");
        return toDTO(order);
    }

    @Transactional
    @Override
    public MembershipOrderDTO payOrder(Long userId, String orderNo) {
        MembershipOrder order = requireOrderByNo(orderNo);
        if (!Objects.equals(order.getUser_id_wsh(), userId)) {
            throw new BusinessException(403, "无权支付该会员订单");
        }
        return payOrderInternal(order, userId, false);
    }

    @Transactional
    @Override
    public MembershipOrderDTO confirmPaidForAdmin(Long operatorId, String orderNo) {
        MembershipOrder order = requireOrderByNo(orderNo);
        return payOrderInternal(order, operatorId, true);
    }

    @Override
    public MembershipOrderDTO toDTO(MembershipOrder order) {
        if (order == null) {
            return null;
        }
        MembershipOrderDTO dto = new MembershipOrderDTO();
        BeanUtils.copyProperties(order, dto);
        applySnapshot(dto, order.getPlan_snapshot_wsh());
        return dto;
    }

    private MemberPlan requireUsablePlan(Long planId, String planCode) {
        MemberPlan plan = null;
        if (planId != null) {
            plan = memberPlanMapper.selectById(planId);
        } else if (planCode != null && !planCode.isBlank()) {
            plan = memberPlanMapper.selectOne(new LambdaQueryWrapper<MemberPlan>()
                    .eq(MemberPlan::getCode_wsh, planCode.trim().toUpperCase(Locale.ROOT))
                    .last("LIMIT 1"));
        }
        if (plan == null) {
            throw new BusinessException(404, "会员套餐不存在");
        }
        if (plan.getStatus_wsh() == null || plan.getStatus_wsh() != PLAN_ENABLED) {
            throw new BusinessException(400, "会员套餐已停用");
        }
        if (plan.getDuration_days_wsh() == null || plan.getDuration_days_wsh() <= 0) {
            throw new BusinessException(400, "会员套餐有效期配置错误");
        }
        return plan;
    }

    private MembershipOrderDTO payOrderInternal(MembershipOrder order, Long operatorId, boolean adminOperation) {
        if (STATUS_PAID.equals(order.getStatus_wsh())) {
            activateMembership(order, operatorId, adminOperation, false);
            return toDTO(order);
        }
        if (!STATUS_PENDING.equals(order.getStatus_wsh())) {
            throw new BusinessException(400, "当前会员订单状态不可支付");
        }
        if (!adminOperation) {
            ensureUserPayableMethod(order.getPay_method_wsh());
            settleBalancePayment(order);
        }
        LocalDateTime paidAt = LocalDateTime.now();
        MembershipOrder update = new MembershipOrder();
        update.setStatus_wsh(STATUS_PAID);
        update.setPaid_at_wsh(paidAt);
        int updated = membershipOrderMapper.update(update, new LambdaUpdateWrapper<MembershipOrder>()
                .eq(MembershipOrder::getId_wsh, order.getId_wsh())
                .eq(MembershipOrder::getStatus_wsh, STATUS_PENDING));
        if (updated == 0) {
            MembershipOrder latest = membershipOrderMapper.selectById(order.getId_wsh());
            if (latest != null && STATUS_PAID.equals(latest.getStatus_wsh())) {
                activateMembership(latest, operatorId, adminOperation, false);
                return toDTO(latest);
            }
            throw new BusinessException(400, "会员订单状态已变化，请刷新后重试");
        }
        order.setStatus_wsh(STATUS_PAID);
        order.setPaid_at_wsh(paidAt);
        createEvent(order, "order_paid", adminOperation ? "管理员确认会员订单支付" : "会员订单支付成功");
        activateMembership(order, operatorId, adminOperation, true);
        return toDTO(order);
    }

    private void activateMembership(MembershipOrder order, Long operatorId, boolean adminOperation, boolean statusChanged) {
        // 教学注释：会员续费是“读当前到期时间 -> 计算新到期时间 -> 写回”的流程。
        // 同一用户并发支付两笔订单时必须串行，否则两边都会基于旧到期时间计算，导致少续一次。
        synchronized (activationLock(order.getUser_id_wsh())) {
            activateMembershipLocked(order, operatorId, adminOperation, statusChanged);
        }
    }

    private void activateMembershipLocked(MembershipOrder order, Long operatorId, boolean adminOperation, boolean statusChanged) {
        UserMembership existing = userMembershipMapper.selectOne(new LambdaQueryWrapper<UserMembership>()
                .eq(UserMembership::getUser_id_wsh, order.getUser_id_wsh())
                .last("LIMIT 1"));
        if (existing != null
                && Objects.equals(existing.getLast_order_id_wsh(), order.getId_wsh())
                && "active".equals(existing.getStatus_wsh())) {
            return;
        }

        MemberPlan plan = memberPlanMapper.selectById(order.getPlan_id_wsh());
        ActivationPeriod period = resolveActivationPeriod(order, plan, existing);
        UserMembership membership = existing == null ? new UserMembership() : existing;
        membership.setUser_id_wsh(order.getUser_id_wsh());
        membership.setPlan_id_wsh(order.getPlan_id_wsh());
        membership.setPlan_code_wsh(order.getPlan_code_wsh());
        membership.setLevel_wsh(resolveLevel(plan, order.getPlan_snapshot_wsh()));
        membership.setStatus_wsh("active");
        membership.setStarted_at_wsh(period.startAt());
        membership.setExpires_at_wsh(period.endAt());
        membership.setAuto_renew_wsh(membership.getAuto_renew_wsh() == null ? 0 : membership.getAuto_renew_wsh());
        membership.setSource_wsh(adminOperation ? "admin_confirm" : "purchase");
        membership.setLast_order_id_wsh(order.getId_wsh());
        membership.setBenefit_snapshot_wsh(order.getPlan_snapshot_wsh());
        if (membership.getId_wsh() == null) {
            userMembershipMapper.insert(membership);
        } else {
            userMembershipMapper.updateById(membership);
        }

        if (!Objects.equals(order.getMembership_start_at_wsh(), period.startAt())
                || !Objects.equals(order.getMembership_end_at_wsh(), period.endAt())) {
            MembershipOrder orderUpdate = new MembershipOrder();
            orderUpdate.setId_wsh(order.getId_wsh());
            orderUpdate.setMembership_start_at_wsh(period.startAt());
            orderUpdate.setMembership_end_at_wsh(period.endAt());
            membershipOrderMapper.updateById(orderUpdate);
            order.setMembership_start_at_wsh(period.startAt());
            order.setMembership_end_at_wsh(period.endAt());
        }

        createMembershipEvent(membership, order, statusChanged ? "membership_activated" : "membership_repaired",
                operatorId, statusChanged ? "会员已开通或续费" : "已补偿会员状态");
    }

    private Object activationLock(Long userId) {
        return activationLocks[Math.floorMod(Long.hashCode(userId == null ? 0L : userId), activationLocks.length)];
    }

    private static Object[] createLockShards() {
        Object[] locks = new Object[ACTIVATION_LOCK_SHARDS];
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new Object();
        }
        return locks;
    }

    private ActivationPeriod resolveActivationPeriod(MembershipOrder order, MemberPlan plan, UserMembership existing) {
        LocalDateTime now = LocalDateTime.now();
        long durationDays = resolveDurationDays(order, plan);
        LocalDateTime startAt = order.getMembership_start_at_wsh() == null ? now : order.getMembership_start_at_wsh();
        if (existing != null
                && "active".equals(existing.getStatus_wsh())
                && existing.getExpires_at_wsh() != null
                && existing.getExpires_at_wsh().isAfter(now)
                && !Objects.equals(existing.getLast_order_id_wsh(), order.getId_wsh())
                && existing.getExpires_at_wsh().isAfter(startAt)) {
            startAt = existing.getExpires_at_wsh();
        }
        if (startAt.isBefore(now)
                && (existing == null || existing.getExpires_at_wsh() == null || !existing.getExpires_at_wsh().isAfter(now))) {
            startAt = now;
        }
        return new ActivationPeriod(startAt, startAt.plusDays(durationDays));
    }

    private long resolveDurationDays(MembershipOrder order, MemberPlan plan) {
        if (order.getMembership_start_at_wsh() != null
                && order.getMembership_end_at_wsh() != null
                && order.getMembership_end_at_wsh().isAfter(order.getMembership_start_at_wsh())) {
            return Math.max(1L, ChronoUnit.DAYS.between(
                    order.getMembership_start_at_wsh().toLocalDate(),
                    order.getMembership_end_at_wsh().toLocalDate()));
        }
        if (plan != null && plan.getDuration_days_wsh() != null && plan.getDuration_days_wsh() > 0) {
            return plan.getDuration_days_wsh();
        }
        return 1L;
    }

    private Integer resolveLevel(MemberPlan plan, String snapshot) {
        if (plan != null && plan.getLevel_wsh() != null) {
            return plan.getLevel_wsh();
        }
        if (snapshot != null && !snapshot.isBlank()) {
            try {
                JsonNode node = objectMapper.readTree(snapshot).get("level_wsh");
                if (node != null && node.canConvertToInt()) {
                    return node.asInt();
                }
            } catch (Exception ignored) {
            }
        }
        return 0;
    }

    private LocalDateTime resolveStartAt(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        UserMembership membership = userMembershipMapper.selectOne(new LambdaQueryWrapper<UserMembership>()
                .eq(UserMembership::getUser_id_wsh, userId)
                .eq(UserMembership::getStatus_wsh, "active")
                .gt(UserMembership::getExpires_at_wsh, now)
                .orderByDesc(UserMembership::getExpires_at_wsh)
                .last("LIMIT 1"));
        if (membership != null && membership.getExpires_at_wsh() != null) {
            return membership.getExpires_at_wsh();
        }
        return now;
    }

    private MembershipOrder requireOrderByNo(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            throw new BusinessException(400, "会员订单号不能为空");
        }
        MembershipOrder order = membershipOrderMapper.selectOne(new LambdaQueryWrapper<MembershipOrder>()
                .eq(MembershipOrder::getOrder_no_wsh, orderNo.trim())
                .last("LIMIT 1"));
        if (order == null) {
            throw new BusinessException(404, "会员订单不存在");
        }
        return order;
    }

    private MembershipOrder findByRequestId(String requestId) {
        return membershipOrderMapper.selectOne(new LambdaQueryWrapper<MembershipOrder>()
                .eq(MembershipOrder::getRequest_id_wsh, requestId)
                .last("LIMIT 1"));
    }

    private void createEvent(MembershipOrder order, String eventType, String message) {
        MembershipEvent event = new MembershipEvent();
        event.setUser_id_wsh(order.getUser_id_wsh());
        event.setMembership_order_id_wsh(order.getId_wsh());
        event.setEvent_type_wsh(eventType);
        event.setEvent_status_wsh("success");
        event.setMessage_wsh(message);
        event.setEvent_snapshot_wsh(toEventSnapshot(order));
        membershipEventMapper.insert(event);
    }

    private void createMembershipEvent(UserMembership membership, MembershipOrder order, String eventType, Long operatorId, String message) {
        MembershipEvent event = new MembershipEvent();
        event.setUser_id_wsh(order.getUser_id_wsh());
        event.setMembership_id_wsh(membership.getId_wsh());
        event.setMembership_order_id_wsh(order.getId_wsh());
        event.setEvent_type_wsh(eventType);
        event.setEvent_status_wsh("success");
        event.setOperator_id_wsh(operatorId);
        event.setMessage_wsh(message);
        event.setEvent_snapshot_wsh(toEventSnapshot(order));
        membershipEventMapper.insert(event);
    }

    private String toPlanSnapshot(MemberPlan plan) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("plan_id_wsh", plan.getId_wsh());
        data.put("plan_code_wsh", plan.getCode_wsh());
        data.put("plan_name_wsh", plan.getName_wsh());
        data.put("level_wsh", plan.getLevel_wsh());
        data.put("price_wsh", money(plan.getPrice_wsh()));
        data.put("duration_days_wsh", plan.getDuration_days_wsh());
        data.put("discount_rate_wsh", plan.getDiscount_rate_wsh());
        data.put("monthly_coupon_config_wsh", plan.getMonthly_coupon_config_wsh());
        data.put("benefit_config_wsh", plan.getBenefit_config_wsh());
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String toEventSnapshot(MembershipOrder order) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("order_no_wsh", order.getOrder_no_wsh());
        data.put("status_wsh", order.getStatus_wsh());
        data.put("amount_wsh", order.getAmount_wsh());
        data.put("request_id_wsh", order.getRequest_id_wsh());
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }

    private void applySnapshot(MembershipOrderDTO dto, String snapshot) {
        if (snapshot == null || snapshot.isBlank()) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(snapshot);
            JsonNode name = node.get("plan_name_wsh");
            if (name != null && !name.isNull()) {
                dto.setPlan_name_wsh(name.asText());
            }
            JsonNode level = node.get("level_wsh");
            if (level != null && level.canConvertToInt()) {
                dto.setLevel_wsh(level.asInt());
            }
        } catch (Exception ignored) {
        }
    }

    private String normalizeRequestId(String requestId) {
        String value = trimToNull(requestId);
        if (value == null) {
            throw new BusinessException(400, "幂等请求ID不能为空");
        }
        if (value.length() > 120) {
            throw new BusinessException(400, "幂等请求ID不能超过120个字符");
        }
        return value;
    }

    private String normalizePayMethod(String method) {
        String value = method == null || method.isBlank() ? METHOD_BALANCE : method.trim().toLowerCase(Locale.ROOT);
        if ("mock".equals(value)) {
            throw new BusinessException(400, "模拟支付已停用");
        }
        if (!SUPPORTED_PAY_METHODS.contains(value)) {
            throw new BusinessException(400, "不支持的支付方式");
        }
        return value;
    }

    private void ensureUserPayableMethod(String method) {
        if (METHOD_BALANCE.equals(method)) {
            return;
        }
        if ("mock".equals(method)) {
            throw new BusinessException(400, "模拟支付已停用");
        }
        throw new BusinessException(400, "该支付方式需等待支付平台确认");
    }

    private void settleBalancePayment(MembershipOrder order) {
        BigDecimal amount = money(order.getAmount_wsh());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        String businessId = String.valueOf(order.getId_wsh());
        String requestId = "membership-payment:" + businessId;
        accountingService.debit(order.getUser_id_wsh(), amount, "membership_payment", null,
                "membership_order", businessId, requestId + ":owner", "会员余额支付 - " + order.getOrder_no_wsh());
        accountingService.credit(accountingService.systemUserId(), amount, "membership_payment", null,
                "membership_order", businessId, requestId + ":system", "会员支付进入系统暂存 - " + order.getOrder_no_wsh());
    }

    private String normalizeStatusOrNull(String status) {
        String value = trimToNull(status);
        if (value == null) {
            return null;
        }
        if (!List.of(STATUS_PENDING, "paid", STATUS_CANCELLED, "refunded").contains(value)) {
            throw new BusinessException(400, "不支持的会员订单状态");
        }
        return value;
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String generateOrderNo() {
        return "MO" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record ActivationPeriod(LocalDateTime startAt, LocalDateTime endAt) {
    }
}
