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
     * 【构造会员订单服务】
     *
     * 业务作用：注入会员订单、会员套餐、用户会员、会员事件四个 Mapper，以及记账服务和 JSON 序列化器。
     *
     * 调用场景：Spring 容器启动创建 Bean 时调用。
     *
     * 调用链：Spring 容器 → 本构造器 → 赋值各 Mapper / AccountingService / ObjectMapper 字段。
     *
     * 数据处理：无。
     *
     * 业务规则：全部依赖均为 final 字段，构造后不可替换。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
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
     * 【业务名称】创建会员订单（实现）
     * 业务作用：用户创建会员购买订单，含幂等处理。
     * 调用场景：用户购买/续费会员。
     * 调用链：createOrder() → 幂等校验 → requireUsablePlan() → resolveStartAt() → insert() → createEvent()。
     * 数据处理：幂等校验 → 校验套餐 → 计算有效期 → 插入 pending 订单 → 记录订单事件。
     * 业务规则：request_id 防重复；续费从现有到期日顺延；仅启用的套餐可购买。
     * 状态影响：新增 pending 订单；记录 order_created 事件。
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
     * 【业务名称】查询我的订单列表（实现）
     * 业务作用：查询用户的所有会员订单，按时间倒序。
     * 调用场景：用户查看购买记录。
     * 调用链：listMyOrders() → selectList() → toDTO()。
     * 数据处理：按 user_id 查询，按时间倒序。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
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
     * 【业务名称】管理端查询订单列表（实现）
     * 业务作用：管理端查询会员订单，可选按状态筛选。
     * 调用场景：后台订单管理。
     * 调用链：listOrdersForAdmin() → selectList() → toDTO()。
     * 数据处理：按状态筛选（可选），按创建时间倒序。
     * 业务规则：状态支持 pending/paid/cancelled/refunded。
     * 状态影响：无。
     * 异常情况：不支持的状态抛 BusinessException(400)。
     * 注意事项：无。
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
     * 【业务名称】查询我的订单详情（实现）
     * 业务作用：根据订单号查询订单详情，含归属校验。
     * 调用场景：用户查看订单详情。
     * 调用链：getMyOrder() → requireOrderByNo() → toDTO()。
     * 数据处理：按订单号查询，校验用户归属。
     * 业务规则：仅订单所属用户可查看。
     * 状态影响：无。
     * 异常情况：订单不存在抛 404；非本人抛 403。
     * 注意事项：无。
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
     * 【业务名称】取消待支付订单（实现）
     * 业务作用：取消待支付的会员订单，使用乐观锁防并发。
     * 调用场景：用户取消未支付订单。
     * 调用链：cancelPendingOrder() → 校验归属和状态 → 乐观锁更新 → createEvent()。
     * 数据处理：乐观锁更新 status=cancelled（条件：id 和 status=pending）。
     * 业务规则：仅订单主人可取消；仅 pending 状态可取消。
     * 状态影响：订单 pending → cancelled；记录 order_cancelled 事件。
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
     * 【业务名称】支付会员订单（实现）
     * 业务作用：用户支付会员订单，余额扣款后激活会员。
     * 调用场景：用户使用余额支付。
     * 调用链：payOrder() → 校验 → payOrderInternal() → settleBalancePayment() → 乐观锁更新 → activateMembership()。
     * 数据处理：校验 → 余额扣款 → 更新订单 → 会员激活。
     * 业务规则：仅 pending 状态可支付；余额扣款通过 AccountingService.debit/credit。
     * 状态影响：订单 paid；会员 active 更新/新增；记录资金流水和事件。
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
     * 【业务名称】管理端确认付费（实现）
     * 业务作用：管理员手动确认订单已付费，跳过余额扣款直接激活会员。
     * 调用场景：后台确认线下转账。
     * 调用链：confirmPaidForAdmin() → payOrderInternal()（adminOperation=true）。
     * 数据处理：跳过扣款 → 更新订单 → 激活会员。
     * 业务规则：仅管理员调用（adminOperation=true 跳过扣款）。
     * 状态影响：订单 paid；会员 active 更新/新增。
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
     * 【业务名称】订单实体转DTO（实现）
     * 业务作用：将订单实体转换为 DTO，关联套餐快照。
     * 调用场景：内部转换。
     * 调用链：toDTO() → BeanUtils.copyProperties() → applySnapshot()。
     * 数据处理：字段拷贝 → 解析 plan_snapshot JSON。
     * 业务规则：入参为 null 时返回 null。
     * 状态影响：无。
     * 异常情况：无。
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
     * 【查询并校验可用会员套餐】
     *
     * 业务作用：按套餐 ID 或套餐编码查出会员套餐，并校验其处于启用状态且有效期配置合法。
     *
     * 调用场景：createOrder 创建会员订单前。
     *
     * 调用链：createOrder() → requireUsablePlan() → memberPlanMapper.selectById() / selectOne()。
     *
     * 数据处理：planId 非空时按主键查询；否则用 trim + 大写后的 planCode 按 code_wsh 查询并 LIMIT 1。
     *
     * 业务规则：status_wsh 必须等于 PLAN_ENABLED(1)；duration_days_wsh 必须大于 0。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：套餐不存在抛 BusinessException(404)；已停用抛 400「会员套餐已停用」；有效期配置错误抛 400。
     *
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
     * 【业务名称】内部支付处理
     * 业务作用：支付订单内部逻辑，含余额扣款、乐观锁更新、会员激活。
     * 调用场景：用户支付或管理员确认。
     * 调用链：payOrderInternal() → settleBalancePayment() → 乐观锁更新 → activateMembership()。
     * 数据处理：管理端跳过扣款 → 更新 paid → 激活会员。
     * 业务规则：仅 pending 状态可支付；管理员操作跳过扣款。
     * 状态影响：订单 paid；会员激活；记录事件。
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
     * 【业务名称】激活会员（分片锁）
     * 业务作用：激活或续费会员，使用分片锁防止并发续费数据不一致。
     * 调用场景：支付成功后激活。
     * 调用链：activateMembership() → synchronized(分片锁) → activateMembershipLocked()。
     * 数据处理：分片锁保护 userId 粒度串行。
     * 业务规则：同一用户并发支付两笔订单时串行处理。
     * 状态影响：会员记录创建或更新。
     * 异常情况：无。
     * 注意事项：分片锁减少锁竞争。
     */
    private void activateMembership(MembershipOrder order, Long operatorId, boolean adminOperation, boolean statusChanged) {
        synchronized (activationLock(order.getUser_id_wsh())) {
            activateMembershipLocked(order, operatorId, adminOperation, statusChanged);
        }
    }

    /**
     * 【业务名称】激活会员（锁内执行）
     * 业务作用：在分片锁内执行会员激活逻辑。
     * 调用场景：分片锁获得后。
     * 调用链：activateMembershipLocked() → 查询/创建会员 → 更新 → 记录事件。
     * 数据处理：查询现有会员 → 计算有效期 → 创建/更新会员 → 更新订单有效期（如变更）→ 记录事件。
     * 业务规则：已激活且订单已关联的会员跳过；续费从现有到期日顺延。
     * 状态影响：会员创建或更新；订单有效期可能更新。
     * 异常情况：无。
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
     * 【取用户分片锁】
     *
     * 业务作用：按 userId 的哈希值取模，从分片锁数组中选出一个锁对象。
     *
     * 调用场景：activateMembership() 加 synchronized 前。
     *
     * 调用链：activateMembership() → activationLock() → activationLocks 数组下标计算。
     *
     * 数据处理：userId 为 null 时按 0L 参与哈希，结果用 Math.floorMod 保证非负。
     *
     * 业务规则：分片数固定为 ACTIVATION_LOCK_SHARDS(64)。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：不同 userId 可能落到同一分片，只能降低锁竞争，不能做到用户级完全隔离。
     */
    private Object activationLock(Long userId) {
        return activationLocks[Math.floorMod(Long.hashCode(userId == null ? 0L : userId), activationLocks.length)];
    }

    /**
     * 【创建分片锁数组】
     *
     * 业务作用：新建 64 个 Object 作为会员激活用的分片锁。
     *
     * 调用场景：Bean 实例化时初始化 activationLocks 字段。
     *
     * 调用链：字段初始化 activationLocks = createLockShards() → 逐个填充 new Object()。
     *
     * 数据处理：创建长度为 ACTIVATION_LOCK_SHARDS 的数组并逐位赋值。
     *
     * 业务规则：数组长度由常量 ACTIVATION_LOCK_SHARDS 决定。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
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
     * 【计算激活有效期区间】
     *
     * 业务作用：计算本次会员激活的实际起止时间，处理续费顺延、过期重开两种情况。
     *
     * 调用场景：activateMembershipLocked() 写入用户会员有效期前。
     *
     * 调用链：activateMembershipLocked() → resolveActivationPeriod() → resolveDurationDays()。
     *
     * 数据处理：以订单 membership_start_at 为基准（为空取当前时间）；若已有 active 且未过期的会员、且不是同一订单、且原到期日晚于基准，则从原到期日顺延。
     *
     * 业务规则：基准时间早于当前且用户没有有效会员时，起算时间重置为当前时间；时长由 resolveDurationDays 决定。
     *
     * 状态影响：无，纯计算，返回 ActivationPeriod 记录。
     *
     * 异常情况：无。
     *
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
     * 【解析会员时长（天）】
     *
     * 业务作用：确定本次激活应发放的会员天数。
     *
     * 调用场景：resolveActivationPeriod() 计算结束时间。
     *
     * 调用链：resolveActivationPeriod() → resolveDurationDays()。
     *
     * 数据处理：订单起止时间都存在且结束晚于开始时，按两个 LocalDate 的天数差计算；否则取套餐 duration_days_wsh；都没有则返回 1。
     *
     * 业务规则：返回值最小为 1 天。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
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
     * 【解析会员等级】
     *
     * 业务作用：确定写入用户会员记录的等级值。
     *
     * 调用场景：activateMembershipLocked() 设置 membership.level_wsh。
     *
     * 调用链：activateMembershipLocked() → resolveLevel() → objectMapper.readTree()。
     *
     * 数据处理：套餐 level_wsh 非空直接返回；否则读订单套餐快照 JSON 中的 level_wsh 字段。
     *
     * 业务规则：快照缺失、解析失败或字段不可转 int 时返回 0。
     *
     * 状态影响：无。
     *
     * 异常情况：JSON 解析异常被吞掉，不向外抛出。
     *
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
     * 【计算下单时的会员生效时间】
     *
     * 业务作用：创建订单时确定会员生效时间，续费场景从现有会员到期日顺延。
     *
     * 调用场景：createOrder() 写入订单 membership_start_at 前。
     *
     * 调用链：createOrder() → resolveStartAt() → userMembershipMapper.selectOne()。
     *
     * 数据处理：查该用户 status=active 且 expires_at 晚于当前时间的会员，按到期日倒序取第一条，取其 expires_at。
     *
     * 业务规则：有有效会员则从其到期日开始，否则从当前时间开始。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：无。
     *
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
     * 【按订单号查询会员订单】
     *
     * 业务作用：根据订单号查出会员订单，为空或不存在时直接抛业务异常。
     *
     * 调用场景：getMyOrder / cancelPendingOrder / payOrder / confirmPaidForAdmin 的入口校验。
     *
     * 调用链：getMyOrder() / cancelPendingOrder() / payOrder() / confirmPaidForAdmin() → requireOrderByNo() → membershipOrderMapper.selectOne()。
     *
     * 数据处理：orderNo trim 后按 order_no_wsh 精确匹配，LIMIT 1。
     *
     * 业务规则：订单号不能为空白。
     *
     * 状态影响：无。
     *
     * 异常情况：订单号为空抛 BusinessException(400)；查不到抛 BusinessException(404)「会员订单不存在」。
     *
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
     * 【按幂等请求ID查询订单】
     *
     * 业务作用：根据幂等请求 ID 查询已存在的会员订单。
     *
     * 调用场景：createOrder 的幂等预检，以及 DuplicateKeyException 后的兜底查询。
     *
     * 调用链：createOrder() → findByRequestId() → membershipOrderMapper.selectOne()。
     *
     * 数据处理：按 request_id_wsh 精确匹配，LIMIT 1。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无，查不到返回 null。
     *
     * 注意事项：方法本身不做空值校验，调用前 requestId 已由 normalizeRequestId 保证非空。
     */
    private MembershipOrder findByRequestId(String requestId) {
        return membershipOrderMapper.selectOne(new LambdaQueryWrapper<MembershipOrder>()
                .eq(MembershipOrder::getRequest_id_wsh, requestId)
                .last("LIMIT 1"));
    }

    /**
     * 【记录订单维度会员事件】
     *
     * 业务作用：向会员事件表写入一条订单维度的事件记录。
     *
     * 调用场景：订单创建成功、取消成功、支付成功后。
     *
     * 调用链：createOrder() / cancelPendingOrder() / payOrderInternal() → createEvent() → toEventSnapshot() → membershipEventMapper.insert()。
     *
     * 数据处理：写入 user_id_wsh、membership_order_id_wsh、event_type_wsh、event_status_wsh=success、message_wsh 和事件快照 JSON。
     *
     * 业务规则：事件状态固定为 success，不记录失败事件。
     *
     * 状态影响：新增一条 membership_event 记录。
     *
     * 异常情况：无显式抛出，插入失败由数据库异常向上传播。
     *
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
     * 【记录会员维度激活事件】
     *
     * 业务作用：写入带会员 ID 和操作人的会员激活/补偿事件。
     *
     * 调用场景：activateMembershipLocked() 完成用户会员写入之后。
     *
     * 调用链：activateMembershipLocked() → createMembershipEvent() → toEventSnapshot() → membershipEventMapper.insert()。
     *
     * 数据处理：写入 user_id_wsh、membership_id_wsh、membership_order_id_wsh、event_type_wsh、operator_id_wsh、message_wsh 与事件快照。
     *
     * 业务规则：本次支付真正改变了订单状态时记 membership_activated，否则记 membership_repaired。
     *
     * 状态影响：新增一条 membership_event 记录。
     *
     * 异常情况：无显式抛出。
     *
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
     * 【生成套餐快照JSON】
     *
     * 业务作用：把会员套餐序列化为 JSON 快照，随订单一起落库。
     *
     * 调用场景：createOrder 写入 order.plan_snapshot_wsh。
     *
     * 调用链：createOrder() → toPlanSnapshot() → objectMapper.writeValueAsString()。
     *
     * 数据处理：按固定顺序写入 plan_id、plan_code、plan_name、level、price、duration_days、discount_rate、monthly_coupon_config、benefit_config。
     *
     * 业务规则：价格经 money() 归一为两位小数。
     *
     * 状态影响：无，仅生成字符串。
     *
     * 异常情况：序列化异常时返回 "{}"。
     *
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
     * 【生成订单事件快照JSON】
     *
     * 业务作用：把订单关键字段序列化为事件快照字符串。
     *
     * 调用场景：createEvent() 与 createMembershipEvent() 写入 event_snapshot_wsh。
     *
     * 调用链：createEvent() / createMembershipEvent() → toEventSnapshot() → objectMapper.writeValueAsString()。
     *
     * 数据处理：包含 order_no_wsh、status_wsh、amount_wsh、request_id_wsh 四个字段。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：序列化异常时返回 "{}"。
     *
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
     * 【回填套餐快照到DTO】
     *
     * 业务作用：从订单的套餐快照 JSON 中取出套餐名与等级回填到 DTO。
     *
     * 调用场景：toDTO() 转换会员订单。
     *
     * 调用链：toDTO() → applySnapshot() → objectMapper.readTree()。
     *
     * 数据处理：读取 plan_name_wsh 与 level_wsh 两个字段写入 DTO。
     *
     * 业务规则：快照为空或空白时直接返回，不改动 DTO 原有字段。
     *
     * 状态影响：无。
     *
     * 异常情况：解析异常被吞掉，DTO 保留原值。
     *
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
     * 【校验幂等请求ID】
     *
     * 业务作用：对幂等请求 ID 做 trim、判空和长度校验。
     *
     * 调用场景：createOrder 入口。
     *
     * 调用链：createOrder() → normalizeRequestId() → trimToNull()。
     *
     * 数据处理：先 trim 去空白，再判断是否为空和长度。
     *
     * 业务规则：不能为空白，长度不能超过 120 个字符。
     *
     * 状态影响：无。
     *
     * 异常情况：为空抛 BusinessException(400)「幂等请求ID不能为空」；超长抛 400。
     *
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
     * 【规范化支付方式】
     *
     * 业务作用：把下单请求中的支付方式归一，并拦截不支持的渠道。
     *
     * 调用场景：createOrder 写入 order.pay_method_wsh 前。
     *
     * 调用链：createOrder() → normalizePayMethod() → SUPPORTED_PAY_METHODS 白名单校验。
     *
     * 数据处理：空值取 balance，否则 trim 后转小写。
     *
     * 业务规则：仅支持 balance / wechat / alipay；mock 已停用。
     *
     * 状态影响：无。
     *
     * 异常情况：传 mock 抛 400「模拟支付已停用」；其他非法值抛 400「不支持的支付方式」。
     *
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
     * 【校验用户可用的支付方式】
     *
     * 业务作用：用户主动支付时校验支付方式，当前只放行余额支付。
     *
     * 调用场景：payOrderInternal 的非管理端支付分支。
     *
     * 调用链：payOrderInternal() → ensureUserPayableMethod() → settleBalancePayment()。
     *
     * 数据处理：无。
     *
     * 业务规则：balance 直接放行；mock 与 wechat / alipay 一律拒绝。
     *
     * 状态影响：无。
     *
     * 异常情况：mock 抛 400「模拟支付已停用」；其他非余额渠道抛 400「该支付方式需等待支付平台确认」。
     *
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
     * 【余额支付扣款】
     *
     * 业务作用：从用户账户扣除会员费，并转入系统暂存账户。
     *
     * 调用场景：payOrderInternal 的用户支付分支。
     *
     * 调用链：payOrderInternal() → settleBalancePayment() → accountingService.debit() / credit() / systemUserId()。
     *
     * 数据处理：金额经 money() 归一；requestId 拼为 membership-payment:{orderId}，用户侧加 :owner，系统侧加 :system。
     *
     * 业务规则：金额小于等于 0 时直接跳过；业务类型 membership_payment，关联业务 membership_order。
     *
     * 状态影响：写两笔资金流水（用户扣款、系统入账）。
     *
     * 异常情况：余额不足等异常由 AccountingService 抛出并向上传播。
     *
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
     * 【规范化订单状态筛选值】
     *
     * 业务作用：校验管理端传入的订单状态，非法值直接拒绝。
     *
     * 调用场景：listOrdersForAdmin()。
     *
     * 调用链：listOrdersForAdmin() → normalizeStatusOrNull() → trimToNull()。
     *
     * 数据处理：空值返回 null，表示查询不过滤状态。
     *
     * 业务规则：仅允许 pending / paid / cancelled / refunded。
     *
     * 状态影响：无。
     *
     * 异常情况：非法状态抛 BusinessException(400)「不支持的会员订单状态」。
     *
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
     * 【金额归一】
     *
     * 业务作用：把金额统一成两位小数，null 视为 0。
     *
     * 调用场景：订单金额、套餐快照、余额扣款金额等处。
     *
     * 调用链：createOrder() / toPlanSnapshot() / settleBalancePayment() → money()。
     *
     * 数据处理：null 转 0，否则按 HALF_UP 保留两位小数。
     *
     * 业务规则：统一四舍五入。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：只处理精度，不做正负校验。
     */
    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 【生成会员订单号】
     *
     * 业务作用：生成形如 MO+时间戳+随机串的会员订单号。
     *
     * 调用场景：createOrder 新建订单时。
     *
     * 调用链：createOrder() → generateOrderNo()。
     *
     * 数据处理：前缀 MO + yyyyMMddHHmmss 时间 + 去掉横线后取前 8 位的大写 UUID。
     *
     * 业务规则：方法本身不查重，靠数据库唯一索引兜底。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：前缀 MO 与商品订单的 ORD 前缀不同，便于区分业务线。
     */
    private String generateOrderNo() {
        return "MO" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }

    /**
     * 【去空白转null】
     *
     * 业务作用：把字符串 trim 后，空白值统一转成 null。
     *
     * 调用场景：备注、请求 ID、状态等入参清洗。
     *
     * 调用链：createOrder() / normalizeRequestId() / normalizeStatusOrNull() → trimToNull()。
     *
     * 数据处理：null 或全空白返回 null，否则返回 trim 后的值。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
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
