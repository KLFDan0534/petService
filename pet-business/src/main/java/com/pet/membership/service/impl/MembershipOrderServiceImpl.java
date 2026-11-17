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

/**
 * 【业务模块】会员订单管理（实现）
 * 业务作用：提供会员购买订单的创建、支付、取消、确认等全流程管理。
 * 支持幂等创建、余额支付、续费自动衔接、管理员确认和会员激活。
 * 会员激活使用分片锁防止并发续费出错。
 */
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

    /**
     * 业务作用：注入会员订单、会员套餐、用户会员、会员事件四个 Mapper，以及记账服务和 JSON 序列化器。
     * 业务规则：全部依赖均为 final 字段，构造后不可替换。
     * 注意事项：实例字段 activationLocks 在构造器执行前已由 createLockShards() 初始化为 64 个分片锁。
     */
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

    /**
     * 创建会员订单（实现）
     *
     * 业务作用：用户创建会员购买订单，含幂等处理。
     * 调用链：createOrder() → 幂等校验 → requireUsablePlan() → resolveStartAt() → insert() → createEvent()。
     * 数据处理：幂等校验 → 校验套餐 → 计算有效期 → 插入 pending 订单 → 记录订单事件。
     * 业务规则：request_id 防重复；续费从现有到期日顺延；仅启用的套餐可购买。
     * 异常情况：未登录抛 401；请求为空抛 400；套餐不可用抛 404/400；幂等冲突抛 400/403。
     * 注意事项：@Transactional 保证事务一致性。
     */
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
        // 幂等预检：同一请求ID直接复用旧订单
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
        // 唯一索引冲突时回查，兼容并发重复提交
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

    /**
     * 查询我的订单列表（实现）
     *
     * 业务作用：查询用户的所有会员订单，按时间倒序。
     */
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

    /**
     * 管理端查询订单列表（实现）
     *
     * 业务作用：管理端查询会员订单，可选按状态筛选。
     * 业务规则：状态支持 pending/paid/cancelled/refunded。
     * 异常情况：不支持的状态抛 BusinessException(400)。
     */
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

    /**
     * 查询我的订单详情（实现）
     *
     * 业务作用：根据订单号查询订单详情，含归属校验。
     * 业务规则：仅订单所属用户可查看。
     * 异常情况：订单不存在抛 404；非本人抛 403。
     */
    @Override
    public MembershipOrderDTO getMyOrder(Long userId, String orderNo) {
        MembershipOrder order = requireOrderByNo(orderNo);
        if (!Objects.equals(order.getUser_id_wsh(), userId)) {
            throw new BusinessException(403, "无权访问该会员订单");
        }
        return toDTO(order);
    }

    /**
     * 取消待支付订单（实现）
     *
     * 业务作用：取消待支付的会员订单，使用乐观锁防并发。
     * 调用链：cancelPendingOrder() → 校验归属和状态 → 乐观锁更新 → createEvent()。
     * 数据处理：乐观锁更新 status=cancelled（条件：id 和 status=pending）。
     * 业务规则：仅订单主人可取消；仅 pending 状态可取消。
     * 异常情况：非本人抛 403；状态不可取消抛 400；并发冲突抛 400。
     * 注意事项：@Transactional 保证事务一致性。
     */
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
        // 乐观锁失败说明状态已被并发修改
        if (updated == 0) {
            throw new BusinessException(400, "会员订单状态已变化，请刷新后重试");
        }
        order.setStatus_wsh(STATUS_CANCELLED);
        createEvent(order, "order_cancelled", "取消待支付会员订单");
        return toDTO(order);
    }

    /**
     * 支付会员订单（实现）
     *
     * 业务作用：用户支付会员订单，余额扣款后激活会员。
     * 调用链：payOrder() → 校验 → payOrderInternal() → settleBalancePayment() → 乐观锁更新 → activateMembership()。
     * 业务规则：仅 pending 状态可支付；余额扣款通过 AccountingService.debit/credit。
     * 异常情况：非本人抛 403；状态不对抛 400；并发冲突抛 400。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    @Override
    public MembershipOrderDTO payOrder(Long userId, String orderNo) {
        MembershipOrder order = requireOrderByNo(orderNo);
        if (!Objects.equals(order.getUser_id_wsh(), userId)) {
            throw new BusinessException(403, "无权支付该会员订单");
        }
        return payOrderInternal(order, userId, false);
    }

    /**
     * 管理端确认付费（实现）
     *
     * 业务作用：管理员手动确认订单已付费，跳过余额扣款直接激活会员。
     * 业务规则：仅管理员调用（adminOperation=true 跳过扣款）。
     * 异常情况：订单不存在抛异常。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    @Override
    public MembershipOrderDTO confirmPaidForAdmin(Long operatorId, String orderNo) {
        MembershipOrder order = requireOrderByNo(orderNo);
        return payOrderInternal(order, operatorId, true);
    }

    /**
     * 订单实体转DTO（实现）
     *
     * 业务作用：将订单实体转换为 DTO，关联套餐快照。
     * 业务规则：入参为 null 时返回 null。
     * 注意事项：快照用于展示历史套餐名称和等级。
     */
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

    /**
     * 业务作用：按套餐 ID 或套餐编码查出会员套餐，并校验其处于启用状态且有效期配置合法。
     * 数据处理：planId 非空时按主键查询；否则用 trim + 大写后的 planCode 按 code_wsh 查询并 LIMIT 1。
     * 业务规则：status_wsh 必须等于 PLAN_ENABLED(1)；duration_days_wsh 必须大于 0。
     * 异常情况：套餐不存在抛 BusinessException(404)；已停用抛 400「会员套餐已停用」；有效期配置错误抛 400。
     * 注意事项：planCode 查询前会自动 trim 并转大写，调用方无需预处理。
     */
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

    /**
     * 内部支付处理
     *
     * 业务作用：支付订单内部逻辑，含余额扣款、乐观锁更新、会员激活。
     * 调用链：payOrderInternal() → settleBalancePayment() → 乐观锁更新 → activateMembership()。
     * 业务规则：仅 pending 状态可支付；管理员操作跳过扣款。
     * 异常情况：状态不对或并发冲突抛异常。
     * 注意事项：已支付订单直接激活（幂等修复）。
     */
    private MembershipOrderDTO payOrderInternal(MembershipOrder order, Long operatorId, boolean adminOperation) {
        // 已支付订单只补激活，保证重复支付幂等
        if (STATUS_PAID.equals(order.getStatus_wsh())) {
            activateMembership(order, operatorId, adminOperation, false);
            return toDTO(order);
        }
        if (!STATUS_PENDING.equals(order.getStatus_wsh())) {
            throw new BusinessException(400, "当前会员订单状态不可支付");
        }
        // 管理员确认跳过余额扣款
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
        // 并发下已被他人支付时按已支付处理
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

    /**
     * 激活会员（分片锁）
     *
     * 业务作用：激活或续费会员，使用分片锁防止并发续费数据不一致。
     * 业务规则：同一用户并发支付两笔订单时串行处理。
     * 注意事项：分片锁减少锁竞争。
     */
    private void activateMembership(MembershipOrder order, Long operatorId, boolean adminOperation, boolean statusChanged) {
        synchronized (activationLock(order.getUser_id_wsh())) {
            activateMembershipLocked(order, operatorId, adminOperation, statusChanged);
        }
    }

    /**
     * 激活会员（锁内执行）
     *
     * 业务作用：在分片锁内执行会员激活逻辑。
     * 业务规则：已激活且订单已关联的会员跳过；续费从现有到期日顺延。
     * 注意事项：幂等性保护。
     */
    private void activateMembershipLocked(MembershipOrder order, Long operatorId, boolean adminOperation, boolean statusChanged) {
        UserMembership existing = userMembershipMapper.selectOne(new LambdaQueryWrapper<UserMembership>()
                .eq(UserMembership::getUser_id_wsh, order.getUser_id_wsh())
                .last("LIMIT 1"));
        // 同一订单重复激活直接跳过，保证幂等
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

        // 有效期有变化时回写订单，保持两边一致
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

    /**
     * 业务作用：按 userId 的哈希值取模，从分片锁数组中选出一个锁对象。
     * 业务规则：分片数固定为 ACTIVATION_LOCK_SHARDS(64)。
     * 注意事项：不同 userId 可能落到同一分片，只能降低锁竞争，不能做到用户级完全隔离。
     */
    private Object activationLock(Long userId) {
        return activationLocks[Math.floorMod(Long.hashCode(userId == null ? 0L : userId), activationLocks.length)];
    }

    /**
     * 业务作用：新建 64 个 Object 作为会员激活用的分片锁。
     * 业务规则：数组长度由常量 ACTIVATION_LOCK_SHARDS 决定。
     * 注意事项：静态方法，每个 Service 实例各自持有一份锁数组。
     */
    private static Object[] createLockShards() {
        Object[] locks = new Object[ACTIVATION_LOCK_SHARDS];
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new Object();
        }
        return locks;
    }

    /**
     * 业务作用：计算本次会员激活的实际起止时间，处理续费顺延、过期重开两种情况。
     * 业务规则：基准时间早于当前且用户没有有效会员时，起算时间重置为当前时间；时长由 resolveDurationDays 决定。
     * 注意事项：与 createOrder 中的 resolveStartAt 算法不同，这里额外考虑了同一订单重复激活不重复顺延。
     */
    private ActivationPeriod resolveActivationPeriod(MembershipOrder order, MemberPlan plan, UserMembership existing) {
        LocalDateTime now = LocalDateTime.now();
        long durationDays = resolveDurationDays(order, plan);
        LocalDateTime startAt = order.getMembership_start_at_wsh() == null ? now : order.getMembership_start_at_wsh();
        // 有效会员续费从原到期日顺延
        if (existing != null
                && "active".equals(existing.getStatus_wsh())
                && existing.getExpires_at_wsh() != null
                && existing.getExpires_at_wsh().isAfter(now)
                && !Objects.equals(existing.getLast_order_id_wsh(), order.getId_wsh())
                && existing.getExpires_at_wsh().isAfter(startAt)) {
            startAt = existing.getExpires_at_wsh();
        }
        // 起算时间已过期且无有效会员时改用当前时间
        if (startAt.isBefore(now)
                && (existing == null || existing.getExpires_at_wsh() == null || !existing.getExpires_at_wsh().isAfter(now))) {
            startAt = now;
        }
        return new ActivationPeriod(startAt, startAt.plusDays(durationDays));
    }

    /**
     * 业务作用：确定本次激活应发放的会员天数。
     * 数据处理：订单起止时间都存在且结束晚于开始时，按两个 LocalDate 的天数差计算；否则取套餐 duration_days_wsh；都没有则返回 1。
     * 业务规则：返回值最小为 1 天。
     * 注意事项：按日期差计算，时分秒差异会被抹平。
     */
    private long resolveDurationDays(MembershipOrder order, MemberPlan plan) {
        // 优先按订单已定有效期计算天数
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

    /**
     * 业务作用：确定写入用户会员记录的等级值。
     * 数据处理：套餐 level_wsh 非空直接返回；否则读订单套餐快照 JSON 中的 level_wsh 字段。
     * 业务规则：快照缺失、解析失败或字段不可转 int 时返回 0。
     * 异常情况：JSON 解析异常被吞掉，不向外抛出。
     * 注意事项：套餐被删除后仍可从快照恢复等级。
     */
    private Integer resolveLevel(MemberPlan plan, String snapshot) {
        if (plan != null && plan.getLevel_wsh() != null) {
            return plan.getLevel_wsh();
        }
        // 套餐已删除时从订单快照恢复等级
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

    /**
     * 业务作用：创建订单时确定会员生效时间，续费场景从现有会员到期日顺延。
     * 业务规则：有有效会员则从其到期日开始，否则从当前时间开始。
     * 注意事项：这里算出的只是订单预期有效期，支付成功激活时会由 resolveActivationPeriod 再校正一次。
     */
    private LocalDateTime resolveStartAt(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        UserMembership membership = userMembershipMapper.selectOne(new LambdaQueryWrapper<UserMembership>()
                .eq(UserMembership::getUser_id_wsh, userId)
                .eq(UserMembership::getStatus_wsh, "active")
                .gt(UserMembership::getExpires_at_wsh, now)
                .orderByDesc(UserMembership::getExpires_at_wsh)
                .last("LIMIT 1"));
        // 有效会员未到期则从其到期日续期
        if (membership != null && membership.getExpires_at_wsh() != null) {
            return membership.getExpires_at_wsh();
        }
        return now;
    }

    /**
     * 业务作用：根据订单号查出会员订单，为空或不存在时直接抛业务异常。
     * 业务规则：订单号不能为空白。
     * 异常情况：订单号为空抛 BusinessException(400)；查不到抛 BusinessException(404)「会员订单不存在」。
     * 注意事项：不校验归属，归属校验由各调用方分别完成。
     */
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

    /**
     * 业务作用：根据幂等请求 ID 查询已存在的会员订单。
     * 调用场景：createOrder 的幂等预检，以及 DuplicateKeyException 后的兜底查询。
     * 异常情况：无，查不到返回 null。
     * 注意事项：方法本身不做空值校验，调用前 requestId 已由 normalizeRequestId 保证非空。
     */
    private MembershipOrder findByRequestId(String requestId) {
        return membershipOrderMapper.selectOne(new LambdaQueryWrapper<MembershipOrder>()
                .eq(MembershipOrder::getRequest_id_wsh, requestId)
                .last("LIMIT 1"));
    }

    /**
     * 业务作用：向会员事件表写入一条订单维度的事件记录。
     * 业务规则：事件状态固定为 success，不记录失败事件。
     * 异常情况：无显式抛出，插入失败由数据库异常向上传播。
     * 注意事项：不写 membership_id 与 operator_id，这两个字段只在 createMembershipEvent 中填充。
     */
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

    /**
     * 业务作用：写入带会员 ID 和操作人的会员激活/补偿事件。
     * 业务规则：本次支付真正改变了订单状态时记 membership_activated，否则记 membership_repaired。
     * 异常情况：无显式抛出。
     * 注意事项：与 createEvent 的差别是多写 membership_id_wsh 和 operator_id_wsh。
     */
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

    /**
     * 业务作用：把会员套餐序列化为 JSON 快照，随订单一起落库。
     * 业务规则：价格经 money() 归一为两位小数。
     * 异常情况：序列化异常时返回 "{}"。
     * 注意事项：快照保证套餐后续改名改价后仍能展示下单时的信息。
     */
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

    /**
     * 业务作用：把订单关键字段序列化为事件快照字符串。
     * 异常情况：序列化异常时返回 "{}"。
     * 注意事项：金额直接取订单字段，不再做精度处理。
     */
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

    /**
     * 业务作用：从订单的套餐快照 JSON 中取出套餐名与等级回填到 DTO。
     * 业务规则：快照为空或空白时直接返回，不改动 DTO 原有字段。
     * 异常情况：解析异常被吞掉，DTO 保留原值。
     * 注意事项：只回填名称与等级，价格等字段仍取订单自身字段。
     */
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

    /**
     * 业务作用：对幂等请求 ID 做 trim、判空和长度校验。
     * 业务规则：不能为空白，长度不能超过 120 个字符。
     * 异常情况：为空抛 BusinessException(400)「幂等请求ID不能为空」；超长抛 400。
     * 注意事项：只做长度限制，不校验字符集。
     */
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

    /**
     * 业务作用：把下单请求中的支付方式归一，并拦截不支持的渠道。
     * 数据处理：空值取 balance，否则 trim 后转小写。
     * 业务规则：仅支持 balance / wechat / alipay；mock 已停用。
     * 异常情况：传 mock 抛 400「模拟支付已停用」；其他非法值抛 400「不支持的支付方式」。
     * 注意事项：允许用 wechat / alipay 下单，但真正支付时会被 ensureUserPayableMethod 拦下。
     */
    private String normalizePayMethod(String method) {
        // 未指定支付方式时默认余额支付
        String value = method == null || method.isBlank() ? METHOD_BALANCE : method.trim().toLowerCase(Locale.ROOT);
        if ("mock".equals(value)) {
            throw new BusinessException(400, "模拟支付已停用");
        }
        if (!SUPPORTED_PAY_METHODS.contains(value)) {
            throw new BusinessException(400, "不支持的支付方式");
        }
        return value;
    }

    /**
     * 业务作用：用户主动支付时校验支付方式，当前只放行余额支付。
     * 业务规则：balance 直接放行；mock 与 wechat / alipay 一律拒绝。
     * 异常情况：mock 抛 400「模拟支付已停用」；其他非余额渠道抛 400「该支付方式需等待支付平台确认」。
     * 注意事项：与 normalizePayMethod 的白名单不一致，下单可用三渠道但支付只支持余额。
     */
    private void ensureUserPayableMethod(String method) {
        // 目前仅放行余额支付
        if (METHOD_BALANCE.equals(method)) {
            return;
        }
        if ("mock".equals(method)) {
            throw new BusinessException(400, "模拟支付已停用");
        }
        throw new BusinessException(400, "该支付方式需等待支付平台确认");
    }

    /**
     * 业务作用：从用户账户扣除会员费，并转入系统暂存账户。
     * 业务规则：金额小于等于 0 时直接跳过；业务类型 membership_payment，关联业务 membership_order。
     * 异常情况：余额不足等异常由 AccountingService 抛出并向上传播。
     * 注意事项：幂等依赖 requestId，重复调用不会重复扣款。
     */
    private void settleBalancePayment(MembershipOrder order) {
        BigDecimal amount = money(order.getAmount_wsh());
        // 零金额订单不产生资金流水
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

    /**
     * 业务作用：校验管理端传入的订单状态，非法值直接拒绝。
     * 业务规则：仅允许 pending / paid / cancelled / refunded。
     * 异常情况：非法状态抛 BusinessException(400)「不支持的会员订单状态」。
     * 注意事项：refunded 用字面量而非 STATUS_* 常量，与类内其他常量风格不一致。
     */
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

    /**
     * 业务作用：把金额统一成两位小数，null 视为 0。
     * 数据处理：null 转 0，否则按 HALF_UP 保留两位小数。
     * 业务规则：统一四舍五入。
     * 注意事项：只处理精度，不做正负校验。
     */
    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 业务作用：生成形如 MO+时间戳+随机串的会员订单号。
     * 业务规则：方法本身不查重，靠数据库唯一索引兜底。
     * 注意事项：前缀 MO 与商品订单的 ORD 前缀不同，便于区分业务线。
     */
    private String generateOrderNo() {
        return "MO" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }

    /**
     * 业务作用：把字符串 trim 后，空白值统一转成 null。
     * 数据处理：null 或全空白返回 null，否则返回 trim 后的值。
     * 注意事项：与 OrderServiceImpl 中的 defaultText / isBlank 语义相近但实现不同。
     */
    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /** 【会员有效期区间】本次激活的起始与结束时间，仅在 activateMembershipLocked 内部传递。 */
    private record ActivationPeriod(LocalDateTime startAt, LocalDateTime endAt) {
    }
}
