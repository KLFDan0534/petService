package com.pet.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.common.BusinessException;
import com.pet.common.StatusCode;
import com.pet.customer.dto.MerchantCustomerServiceApplyRequestDTO;
import com.pet.customer.dto.MerchantCustomerServiceDTO;
import com.pet.customer.dto.MerchantCustomerServiceReviewRequestDTO;
import com.pet.customer.entity.MerchantCustomerService;
import com.pet.customer.mapper.MerchantCustomerServiceMapper;
import com.pet.customer.service.MerchantCustomerServiceService;
import com.pet.system.entity.User;
import com.pet.system.mapper.UserMapper;
import com.pet.system.service.RoleGrantService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 【业务模块】商家客服申请管理（实现）
 * 业务作用：管理用户申请成为商家客服的完整流程。
 * 状态机设计：pending → approved/rejected → resigned/terminated。
 * 审批通过后自动授予 CUSTOMER_SERVICE 角色，辞职或终止后若用户不再有任一商家的客服身份则自动回收该角色。
 */
@Service
public class MerchantCustomerServiceServiceImpl implements MerchantCustomerServiceService {
    /** 超时自动拒绝时写入的审核备注模板（%d 为超时天数） */
    private static final String AUTO_REJECT_NOTE = "商家超过 %d 天未处理，系统自动拒绝";

    private final MerchantCustomerServiceMapper mapper;
    private final MerchantMapper merchantMapper;
    private final UserMapper userMapper;
    private final RoleGrantService roleGrantService;

    /**
     * 【商家客服申请服务实现的构造注入】
     *
     * 业务作用：注入申请记录、商家、用户 Mapper 与角色授予服务。
     *
     * 调用场景：Spring 容器创建 MerchantCustomerServiceServiceImpl 时。
     *
     * 调用链：Spring 容器 → 本构造方法 → 赋值 4 个 final 成员。
     *
     * 数据处理：仅保存依赖引用。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：无。
     */
    public MerchantCustomerServiceServiceImpl(MerchantCustomerServiceMapper mapper,
                                              MerchantMapper merchantMapper,
                                              UserMapper userMapper,
                                              RoleGrantService roleGrantService) {
        this.mapper = mapper;
        this.merchantMapper = merchantMapper;
        this.userMapper = userMapper;
        this.roleGrantService = roleGrantService;
    }

    /**
     * 【业务名称】申请成为客服（实现）
     * 业务作用：用户申请成为指定商家的客服。
     * 调用场景：用户向商家提交客服申请。
     * 调用链：apply() → 校验商家 → 查重 → insert/update。
     * 数据处理：校验商家存在且已批准 → 查重已有记录 → 复用或新建 pending 状态记录。
     * 业务规则：用户不可为商家主人本人；用户不可重复申请已生效的客服；仅已批准的商家可接收申请。
     * 状态影响：新增或复用一条 pending 申请记录。
     * 异常情况：未登录抛 401；商家不存在抛 404；商家主人无需申请抛 400；商家未批准抛 400；已是客服抛 400。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    @Override
    public MerchantCustomerServiceDTO apply(Long userId, MerchantCustomerServiceApplyRequestDTO request) {
        if (userId == null) {
            throw new BusinessException(401, "login required");
        }
        if (request == null || request.getMerchant_id_wsh() == null) {
            throw new BusinessException(400, "merchant_id_wsh is required");
        }
        Merchant merchant = merchantMapper.selectById(request.getMerchant_id_wsh());
        if (merchant == null) {
            throw new BusinessException(404, "merchant not found");
        }
        if (merchant.getUser_id_wsh() != null && merchant.getUser_id_wsh().equals(userId)) {
            throw new BusinessException(400, "merchant owner does not need customer service application");
        }
        if (merchant.getStatus_wsh() == null
                || merchant.getStatus_wsh() != StatusCode.MERCHANT_APPROVED.getValue()) {
            throw new BusinessException(400, "only approved merchants can receive customer service applications");
        }

        MerchantCustomerService existing = mapper.selectOne(new LambdaQueryWrapper<MerchantCustomerService>()
                .eq(MerchantCustomerService::getMerchant_id_wsh, request.getMerchant_id_wsh())
                .eq(MerchantCustomerService::getUser_id_wsh, userId)
                .last("LIMIT 1"));
        // 已有记录则复用，重置为待审核并清空上次审核痕迹
        if (existing != null) {
            if (STATUS_APPROVED.equals(existing.getStatus_wsh())) {
                throw new BusinessException(400, "user is already customer service for this merchant");
            }
            existing.setStatus_wsh(STATUS_PENDING);
            existing.setApplicant_note_wsh(trimToNull(request.getApplicant_note_wsh()));
            existing.setReview_note_wsh(null);
            existing.setReviewer_id_wsh(null);
            existing.setReviewed_at_wsh(null);
            mapper.updateById(existing);
            return toDTO(existing);
        }

        MerchantCustomerService entity = new MerchantCustomerService();
        entity.setMerchant_id_wsh(request.getMerchant_id_wsh());
        entity.setUser_id_wsh(userId);
        entity.setApplicant_note_wsh(trimToNull(request.getApplicant_note_wsh()));
        entity.setStatus_wsh(STATUS_PENDING);
        mapper.insert(entity);
        return toDTO(entity);
    }

    /**
     * 【业务名称】查询我的申请记录（实现）
     * 业务作用：查询当前用户的所有客服申请记录。
     * 调用场景：用户查看自己的申请记录。
     * 调用链：listMine() → mapper.selectList()。
     * 数据处理：按 user_id 匹配，按创建时间倒序。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     */
    @Override
    public List<MerchantCustomerServiceDTO> listMine(Long userId) {
        return toDTOList(mapper.selectList(new LambdaQueryWrapper<MerchantCustomerService>()
                .eq(MerchantCustomerService::getUser_id_wsh, userId)
                .orderByDesc(MerchantCustomerService::getCreated_at_wsh)));
    }

    /**
     * 【业务名称】商家查询待审核申请（实现）
     * 业务作用：商家查询自己商铺待审核的客服申请列表。
     * 调用场景：商家审核客服申请。
     * 调用链：listPendingForMerchant() → requireMerchantOwner() → mapper.selectList()。
     * 数据处理：按商家ID和 pending 状态匹配。
     * 业务规则：仅商家主人可查询。
     * 状态影响：无。
     * 异常情况：非商家主人抛异常。
     * 注意事项：无。
     */
    @Override
    public List<MerchantCustomerServiceDTO> listPendingForMerchant(Long merchantUserId) {
        Merchant merchant = requireMerchantOwner(merchantUserId);
        return toDTOList(mapper.selectList(new LambdaQueryWrapper<MerchantCustomerService>()
                .eq(MerchantCustomerService::getMerchant_id_wsh, merchant.getId_wsh())
                .eq(MerchantCustomerService::getStatus_wsh, STATUS_PENDING)
                .orderByDesc(MerchantCustomerService::getCreated_at_wsh)));
    }

    /**
     * 【业务名称】商家查询已通过客服（实现）
     * 业务作用：商家查询自己商铺已通过的客服列表。
     * 调用场景：商家管理客服团队。
     * 调用链：listApprovedForMerchant() → requireMerchantOwner() → mapper.selectList()。
     * 数据处理：按商家ID和 approved 状态匹配，按审核时间倒序。
     * 业务规则：仅商家主人可查询。
     * 状态影响：无。
     * 异常情况：非商家主人抛异常。
     * 注意事项：无。
     */
    @Override
    public List<MerchantCustomerServiceDTO> listApprovedForMerchant(Long merchantUserId) {
        Merchant merchant = requireMerchantOwner(merchantUserId);
        return toDTOList(mapper.selectList(new LambdaQueryWrapper<MerchantCustomerService>()
                .eq(MerchantCustomerService::getMerchant_id_wsh, merchant.getId_wsh())
                .eq(MerchantCustomerService::getStatus_wsh, STATUS_APPROVED)
                .orderByDesc(MerchantCustomerService::getReviewed_at_wsh)));
    }

    /**
     * 【业务名称】通过客服申请（实现）
     * 业务作用：商家通过客服申请，自动授予 CUSTOMER_SERVICE 角色。
     * 调用场景：商家审批通过客服申请。
     * 调用链：approve() → requirePendingOwnedApplication() → update() → roleGrantService.grantRoleToUser()。
     * 数据处理：更新状态为 approved，记录审核信息；授予角色。
     * 业务规则：仅 pending 状态可审批；仅商家主人可操作。
     * 状态影响：申请状态 approved；用户新增 CUSTOMER_SERVICE 角色。
     * 异常情况：申请不存在或状态不对抛异常。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    @Override
    public MerchantCustomerServiceDTO approve(Long id, Long merchantUserId, MerchantCustomerServiceReviewRequestDTO request) {
        MerchantCustomerService entity = requirePendingOwnedApplication(id, merchantUserId);
        entity.setStatus_wsh(STATUS_APPROVED);
        entity.setReviewer_id_wsh(merchantUserId);
        entity.setReview_note_wsh(request == null ? null : trimToNull(request.getReview_note_wsh()));
        entity.setReviewed_at_wsh(LocalDateTime.now());
        mapper.updateById(entity);
        roleGrantService.grantRoleToUser(entity.getUser_id_wsh(), "CUSTOMER_SERVICE");
        return toDTO(entity);
    }

    /**
     * 【业务名称】拒绝客服申请（实现）
     * 业务作用：商家拒绝客服申请。
     * 调用场景：商家拒绝客服申请。
     * 调用链：reject() → requirePendingOwnedApplication() → update()。
     * 数据处理：更新状态为 rejected，记录审核信息。
     * 业务规则：仅 pending 状态可拒绝；仅商家主人可操作。
     * 状态影响：申请状态 rejected。
     * 异常情况：申请不存在或状态不对抛异常。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    @Override
    public MerchantCustomerServiceDTO reject(Long id, Long merchantUserId, MerchantCustomerServiceReviewRequestDTO request) {
        MerchantCustomerService entity = requirePendingOwnedApplication(id, merchantUserId);
        entity.setStatus_wsh(STATUS_REJECTED);
        entity.setReviewer_id_wsh(merchantUserId);
        entity.setReview_note_wsh(request == null ? null : trimToNull(request.getReview_note_wsh()));
        entity.setReviewed_at_wsh(LocalDateTime.now());
        mapper.updateById(entity);
        return toDTO(entity);
    }

    /**
     * 【业务名称】超时自动拒绝客服申请（实现）
     * 业务作用：批量把商家长期未处理的 pending 申请置为 rejected，为平台提供兜底。
     * 调用场景：MerchantCustomerServiceTimeoutScheduler 每日调用。
     * 调用链：autoRejectStalePending() → mapper.update() 单条批量 UPDATE。
     * 数据处理：以 updated_at_wsh 为基准筛选超期 pending，批量写 rejected + 审核备注 + 审核时间。
     * 业务规则：仅 pending 可被自动拒绝；reviewer_id_wsh 保持 null 表示无人工审核人。
     * 状态影响：申请状态 pending → rejected。
     * 异常情况：timeoutDays 非正数抛 400。
     * 注意事项：@Transactional 保证事务一致性；单条 UPDATE 避免逐行查询。
     */
    @Transactional
    @Override
    public int autoRejectStalePending(int timeoutDays) {
        if (timeoutDays <= 0) {
            throw new BusinessException(400, "timeoutDays must be positive");
        }
        LocalDateTime now = LocalDateTime.now();
        MerchantCustomerService patch = new MerchantCustomerService();
        patch.setStatus_wsh(STATUS_REJECTED);
        patch.setReview_note_wsh(String.format(AUTO_REJECT_NOTE, timeoutDays));
        patch.setReviewed_at_wsh(now);
        // 单条批量更新，避免逐行查询
        return mapper.update(patch, new LambdaQueryWrapper<MerchantCustomerService>()
                .eq(MerchantCustomerService::getStatus_wsh, STATUS_PENDING)
                .lt(MerchantCustomerService::getUpdated_at_wsh, now.minusDays(timeoutDays)));
    }

    /**
     * 【业务名称】客服辞职（实现）
     * 业务作用：客服辞职，自动回收 CUSTOMER_SERVICE 角色。
     * 调用场景：客服主动辞职。
     * 调用链：resign() → requireApprovedApplication() → 校验身份 → update() → revokeCustomerServiceRoleIfNoApprovedMerchant()。
     * 数据处理：更新状态为 resigned；如果用户再无其他商家客服角色则回收。
     * 业务规则：仅申请者本人可操作。
     * 状态影响：申请状态 resigned；可能回收 CUSTOMER_SERVICE 角色。
     * 异常情况：非本人抛 403。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    @Override
    public MerchantCustomerServiceDTO resign(Long id, Long userId) {
        MerchantCustomerService entity = requireApprovedApplication(id);
        if (entity.getUser_id_wsh() == null || !entity.getUser_id_wsh().equals(userId)) {
            throw new BusinessException(403, "only the customer service owner can resign");
        }
        entity.setStatus_wsh(STATUS_RESIGNED);
        entity.setReviewer_id_wsh(userId);
        entity.setReviewed_at_wsh(LocalDateTime.now());
        mapper.updateById(entity);
        // 状态落库后再判断是否回收客服角色
        revokeCustomerServiceRoleIfNoApprovedMerchant(entity.getUser_id_wsh());
        return toDTO(entity);
    }

    /**
     * 【业务名称】商家终止客服（实现）
     * 业务作用：商家终止客服的合作关系，自动回收 CUSTOMER_SERVICE 角色。
     * 调用场景：商家终止客服。
     * 调用链：terminateByMerchant() → requireOwnedApplication() → 校验状态 → update() → revokeCustomerServiceRoleIfNoApprovedMerchant()。
     * 数据处理：更新状态为 terminated；如果用户再无其他商家客服角色则回收。
     * 业务规则：仅已批准的客服可被终止；仅商家主人可操作。
     * 状态影响：申请状态 terminated；可能回收 CUSTOMER_SERVICE 角色。
     * 异常情况：非 approved 状态抛 400。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    @Override
    public MerchantCustomerServiceDTO terminateByMerchant(Long id, Long merchantUserId) {
        MerchantCustomerService entity = requireOwnedApplication(id, merchantUserId);
        if (!STATUS_APPROVED.equals(entity.getStatus_wsh())) {
            throw new BusinessException(400, "only approved customer service can be terminated");
        }
        entity.setStatus_wsh(STATUS_TERMINATED);
        entity.setReviewer_id_wsh(merchantUserId);
        entity.setReviewed_at_wsh(LocalDateTime.now());
        mapper.updateById(entity);
        revokeCustomerServiceRoleIfNoApprovedMerchant(entity.getUser_id_wsh());
        return toDTO(entity);
    }

    /**
     * 【业务名称】获取用户已通过的商家ID（实现）
     * 业务作用：获取用户所有已通过审核的商家ID集合。
     * 调用场景：查询用户可管理商家范围。
     * 调用链：getApprovedMerchantIds() → mapper.selectList()。
     * 数据处理：按 user_id 和 approved 筛选。
     * 业务规则：用户ID为空返回空集合。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：用于权限判断。
     */
    @Override
    public Set<Long> getApprovedMerchantIds(Long userId) {
        if (userId == null) {
            return Set.of();
        }
        return mapper.selectList(new LambdaQueryWrapper<MerchantCustomerService>()
                        .eq(MerchantCustomerService::getUser_id_wsh, userId)
                        .eq(MerchantCustomerService::getStatus_wsh, STATUS_APPROVED))
                .stream()
                .map(MerchantCustomerService::getMerchant_id_wsh)
                .collect(Collectors.toSet());
    }

    /**
     * 【业务名称】判断是否指定商家的客服（实现）
     * 业务作用：判断用户是否为指定商家的已授权客服。
     * 调用场景：权限校验。
     * 调用链：isMerchantCustomerService() → getApprovedMerchantIds()。
     * 数据处理：通过已通过的商家ID集合判断。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：用于权限判断。
     */
    @Override
    public boolean isMerchantCustomerService(Long userId, Long merchantId) {
        return merchantId != null && getApprovedMerchantIds(userId).contains(merchantId);
    }

    /**
     * 【业务名称】查询指定商家的已授权客服用户ID集合（实现）
     * 业务作用：获取服务指定商家的所有已通过审核客服的用户ID。
     * 调用场景：新工单/新投诉创建时通知服务该商家的客服。
     * 调用链：getApprovedCsUserIds() → mapper.selectList()。
     * 数据处理：按 merchant_id + approved 状态筛选。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：用于提醒推送。
     */
    @Override
    public Set<Long> getApprovedCsUserIds(Long merchantId) {
        if (merchantId == null) {
            return Set.of();
        }
        return mapper.selectList(new LambdaQueryWrapper<MerchantCustomerService>()
                        .eq(MerchantCustomerService::getMerchant_id_wsh, merchantId)
                        .eq(MerchantCustomerService::getStatus_wsh, STATUS_APPROVED))
                .stream()
                .map(MerchantCustomerService::getUser_id_wsh)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * 【加载已通过的客服申请】
     *
     * 业务作用：按 ID 加载申请并确认其状态为 approved，供辞职流程使用。
     *
     * 调用场景：resign() 的第一道校验。
     *
     * 调用链：resign() → requireApprovedApplication() → mapper.selectById()。
     *
     * 数据处理：按主键查询申请记录并比对状态。
     *
     * 业务规则：只有 approved 状态的申请可执行辞职。
     *
     * 状态影响：无。
     *
     * 异常情况：ID 为空抛 400；记录不存在抛 404；状态非 approved 抛 400。
     *
     * 注意事项：只校验状态不校验归属，归属校验由调用方 resign() 完成。
     */
    private MerchantCustomerService requireApprovedApplication(Long id) {
        if (id == null) {
            throw new BusinessException(400, "application id is required");
        }
        MerchantCustomerService entity = mapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "customer service application not found");
        }
        if (!STATUS_APPROVED.equals(entity.getStatus_wsh())) {
            throw new BusinessException(400, "only approved customer service can resign");
        }
        return entity;
    }

    /**
     * 【加载待审核且归属本商家的申请】
     *
     * 业务作用：在归属校验基础上要求申请处于 pending 状态，供审批使用。
     *
     * 调用场景：approve() 与 reject()。
     *
     * 调用链：approve()/reject() → requirePendingOwnedApplication() → requireOwnedApplication()。
     *
     * 数据处理：先做商家归属校验，再比对状态。
     *
     * 业务规则：只有 pending 状态的申请可被审核。
     *
     * 状态影响：无。
     *
     * 异常情况：状态非 pending 抛 BusinessException(400, "only pending applications can be reviewed")。
     *
     * 注意事项：无。
     */
    private MerchantCustomerService requirePendingOwnedApplication(Long id, Long merchantUserId) {
        MerchantCustomerService entity = requireOwnedApplication(id, merchantUserId);
        if (!STATUS_PENDING.equals(entity.getStatus_wsh())) {
            throw new BusinessException(400, "only pending applications can be reviewed");
        }
        return entity;
    }

    /**
     * 【加载并校验申请归属当前商家】
     *
     * 业务作用：确认当前登录用户是商家主人，且申请属于该商家的店铺。
     *
     * 调用场景：requirePendingOwnedApplication() 与 terminateByMerchant()。
     *
     * 调用链：requirePendingOwnedApplication()/terminateByMerchant() → requireOwnedApplication() → requireMerchantOwner() → mapper.selectById()。
     *
     * 数据处理：先按用户 ID 查其名下商家，再按申请 ID 查记录并比对 merchant_id_wsh。
     *
     * 业务规则：非商家主人不可操作；不能操作其他商家的申请。
     *
     * 状态影响：无。
     *
     * 异常情况：ID 为空抛 400；用户非商家主人抛 403；记录不存在抛 404；跨商家操作抛 403。
     *
     * 注意事项：不校验申请状态，状态由上层方法判断。
     */
    private MerchantCustomerService requireOwnedApplication(Long id, Long merchantUserId) {
        if (id == null) {
            throw new BusinessException(400, "application id is required");
        }
        Merchant merchant = requireMerchantOwner(merchantUserId);
        MerchantCustomerService entity = mapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "customer service application not found");
        }
        if (!merchant.getId_wsh().equals(entity.getMerchant_id_wsh())) {
            throw new BusinessException(403, "no permission to review another merchant application");
        }
        return entity;
    }

    /**
     * 【校验并返回当前用户的商家】
     *
     * 业务作用：按登录用户 ID 查询其名下商家，非商家主人则抛异常。
     *
     * 调用场景：listPendingForMerchant、listApprovedForMerchant、requireOwnedApplication。
     *
     * 调用链：各业务方法 → requireMerchantOwner() → merchantMapper.selectOne()。
     *
     * 数据处理：按 merchant.user_id_wsh 查询并 LIMIT 1。
     *
     * 业务规则：查不到商家即视为非商家主人。
     *
     * 状态影响：无。
     *
     * 异常情况：查不到抛 BusinessException(403, "current user is not a merchant owner")。
     *
     * 注意事项：一个账号拥有多个商家时只取第一条，存在多店铺账号归属不确定的问题。
     */
    private Merchant requireMerchantOwner(Long merchantUserId) {
        Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getUser_id_wsh, merchantUserId)
                .last("LIMIT 1"));
        if (merchant == null) {
            throw new BusinessException(403, "current user is not a merchant owner");
        }
        return merchant;
    }

    /**
     * 【按需回收客服角色】
     *
     * 业务作用：当用户不再拥有任一商家的已通过客服身份时，回收其 CUSTOMER_SERVICE 角色。
     *
     * 调用场景：resign() 与 terminateByMerchant() 更新状态之后。
     *
     * 调用链：resign()/terminateByMerchant() → revokeCustomerServiceRoleIfNoApprovedMerchant() → mapper.selectCount() → roleGrantService.revokeRoleFromUser()。
     *
     * 数据处理：统计该用户 approved 状态的申请数，为 0 才回收角色。
     *
     * 业务规则：只要还有一个商家处于 approved 就保留角色；userId 为空直接返回。
     *
     * 状态影响：可能删除 user_role 关联（由 RoleGrantService 写入）。
     *
     * 异常情况：无。
     *
     * 注意事项：必须在状态更新之后调用，否则统计会把刚失效的记录算进去。
     */
    private void revokeCustomerServiceRoleIfNoApprovedMerchant(Long userId) {
        if (userId == null) {
            return;
        }
        Long approvedCount = mapper.selectCount(new LambdaQueryWrapper<MerchantCustomerService>()
                .eq(MerchantCustomerService::getUser_id_wsh, userId)
                .eq(MerchantCustomerService::getStatus_wsh, STATUS_APPROVED));
        // 已无任何商家的客服身份才回收角色
        if (approvedCount == null || approvedCount == 0) {
            roleGrantService.revokeRoleFromUser(userId, "CUSTOMER_SERVICE");
        }
    }

    /**
     * 【业务名称】申请记录实体转DTO
     * 业务作用：将申请记录实体转换为DTO，关联商家名称和用户昵称。
     * 调用场景：对外暴露申请记录信息。
     * 调用链：toDTO()。
     * 数据处理：字段拷贝，关联查询商家名称和用户名。
     * 业务规则：入参为null时返回null。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：关联查询商家和用户表。
     */
    @Override
    public MerchantCustomerServiceDTO toDTO(MerchantCustomerService entity) {
        if (entity == null) {
            return null;
        }
        MerchantCustomerServiceDTO dto = new MerchantCustomerServiceDTO();
        dto.setId_wsh(entity.getId_wsh());
        dto.setMerchant_id_wsh(entity.getMerchant_id_wsh());
        dto.setUser_id_wsh(entity.getUser_id_wsh());
        dto.setApplicant_note_wsh(entity.getApplicant_note_wsh());
        dto.setReview_note_wsh(entity.getReview_note_wsh());
        dto.setStatus_wsh(entity.getStatus_wsh());
        dto.setReviewer_id_wsh(entity.getReviewer_id_wsh());
        dto.setReviewed_at_wsh(entity.getReviewed_at_wsh());
        dto.setCreated_at_wsh(entity.getCreated_at_wsh());
        dto.setUpdated_at_wsh(entity.getUpdated_at_wsh());

        Merchant merchant = entity.getMerchant_id_wsh() == null
                ? null
                : merchantMapper.selectById(entity.getMerchant_id_wsh());
        if (merchant != null) {
            dto.setMerchant_name_wsh(merchant.getName_wsh());
        }

        User user = entity.getUser_id_wsh() == null ? null : userMapper.selectById(entity.getUser_id_wsh());
        if (user != null) {
            dto.setUsername_wsh(user.getUsername_wsh());
            dto.setNickname_wsh(user.getNickname_wsh());
        }
        return dto;
    }

    /**
     * 【申请记录列表转 DTO】
     *
     * 业务作用：批量把申请记录实体转换为 DTO。
     *
     * 调用场景：listMine、listPendingForMerchant、listApprovedForMerchant 返回前。
     *
     * 调用链：各查询方法 → toDTOList() → toDTO()。
     *
     * 数据处理：入参为 null 时返回空列表，否则逐条转换。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：每条记录都会查询商家与用户，列表场景存在 N+1 查询。
     */
    @Override
    public List<MerchantCustomerServiceDTO> toDTOList(List<MerchantCustomerService> list) {
        if (list == null) {
            return List.of();
        }
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 【去空白并转 null】
     *
     * 业务作用：把空白字符串规范化为 null，避免备注字段出现空串。
     *
     * 调用场景：apply()、approve()、reject() 处理申请人备注与审核备注。
     *
     * 调用链：apply()/approve()/reject() → trimToNull()。
     *
     * 数据处理：无文本内容返回 null，否则返回 trim 结果。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：使用 Spring 的 StringUtils.hasText 判断，纯空白也视为空。
     */
    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
