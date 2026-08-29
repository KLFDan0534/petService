package com.pet.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.common.BusinessException;
import com.pet.common.PageRequestDTO;
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
    private final MerchantCustomerServiceMapper mapper;
    private final MerchantMapper merchantMapper;
    private final UserMapper userMapper;
    private final RoleGrantService roleGrantService;

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
     * 【业务名称】管理员分页查询全平台客服申请（实现）
     * 业务作用：管理员查看全平台所有商家的客服申请列表，可按状态筛选。
     * 调用场景：管理后台客服审核列表页。
     * 调用链：pageAll() → mapper.selectPage()。
     * 数据处理：按状态精确筛选（状态为空则查全部），按创建时间倒序分页。
     * 业务规则：不校验商家归属。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：仅供 ADMIN 角色调用。
     */
    @Override
    public IPage<MerchantCustomerService> pageAll(PageRequestDTO pageParam, String status_wsh) {
        LambdaQueryWrapper<MerchantCustomerService> wrapper = new LambdaQueryWrapper<MerchantCustomerService>()
                .eq(org.springframework.util.StringUtils.hasText(status_wsh),
                        MerchantCustomerService::getStatus_wsh, status_wsh)
                .orderByDesc(MerchantCustomerService::getCreated_at_wsh);
        Page<MerchantCustomerService> page = new Page<>(pageParam.getPage(), pageParam.getSize());
        return mapper.selectPage(page, wrapper);
    }

    /**
     * 【业务名称】管理员通过客服申请（实现）
     * 业务作用：管理员通过指定客服申请，自动授予 CUSTOMER_SERVICE 角色。
     * 调用场景：管理后台审核客服申请通过。
     * 调用链：approveByAdmin() → requirePendingApplication() → update() → roleGrantService.grantRoleToUser()。
     * 数据处理：仅 pending 改为 approved，记录审核人和审核时间，授予角色。
     * 业务规则：不校验商家归属。
     * 状态影响：申请状态 approved；用户新增 CUSTOMER_SERVICE 角色。
     * 异常情况：申请不存在抛 404；状态非 pending 抛 400。
     * 注意事项：仅供 ADMIN 角色调用。
     */
    @Transactional
    @Override
    public MerchantCustomerServiceDTO approveByAdmin(Long id, Long adminUserId, String reviewNote) {
        MerchantCustomerService entity = requirePendingApplication(id);
        entity.setStatus_wsh(STATUS_APPROVED);
        entity.setReviewer_id_wsh(adminUserId);
        entity.setReview_note_wsh(trimToNull(reviewNote));
        entity.setReviewed_at_wsh(LocalDateTime.now());
        mapper.updateById(entity);
        if (entity.getUser_id_wsh() != null) {
            roleGrantService.grantRoleToUser(entity.getUser_id_wsh(), "CUSTOMER_SERVICE");
        }
        return toDTO(entity);
    }

    /**
     * 【业务名称】管理员驳回客服申请（实现）
     * 业务作用：管理员驳回指定客服申请。
     * 调用场景：管理后台审核客服申请驳回。
     * 调用链：rejectByAdmin() → requirePendingApplication() → update()。
     * 数据处理：仅 pending 改为 rejected，记录审核人和审核时间。
     * 业务规则：不校验商家归属。
     * 状态影响：申请状态 rejected。
     * 异常情况：申请不存在抛 404；状态非 pending 抛 400。
     * 注意事项：仅供 ADMIN 角色调用。
     */
    @Transactional
    @Override
    public MerchantCustomerServiceDTO rejectByAdmin(Long id, Long adminUserId, String reviewNote) {
        MerchantCustomerService entity = requirePendingApplication(id);
        entity.setStatus_wsh(STATUS_REJECTED);
        entity.setReviewer_id_wsh(adminUserId);
        entity.setReview_note_wsh(trimToNull(reviewNote));
        entity.setReviewed_at_wsh(LocalDateTime.now());
        mapper.updateById(entity);
        return toDTO(entity);
    }

    private MerchantCustomerService requirePendingApplication(Long id) {
        if (id == null) {
            throw new BusinessException(400, "application id is required");
        }
        MerchantCustomerService entity = mapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "customer service application not found");
        }
        if (!STATUS_PENDING.equals(entity.getStatus_wsh())) {
            throw new BusinessException(400, "only pending applications can be reviewed");
        }
        return entity;
    }

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

    private MerchantCustomerService requirePendingOwnedApplication(Long id, Long merchantUserId) {
        MerchantCustomerService entity = requireOwnedApplication(id, merchantUserId);
        if (!STATUS_PENDING.equals(entity.getStatus_wsh())) {
            throw new BusinessException(400, "only pending applications can be reviewed");
        }
        return entity;
    }

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

    private Merchant requireMerchantOwner(Long merchantUserId) {
        Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getUser_id_wsh, merchantUserId)
                .last("LIMIT 1"));
        if (merchant == null) {
            throw new BusinessException(403, "current user is not a merchant owner");
        }
        return merchant;
    }

    private void revokeCustomerServiceRoleIfNoApprovedMerchant(Long userId) {
        if (userId == null) {
            return;
        }
        Long approvedCount = mapper.selectCount(new LambdaQueryWrapper<MerchantCustomerService>()
                .eq(MerchantCustomerService::getUser_id_wsh, userId)
                .eq(MerchantCustomerService::getStatus_wsh, STATUS_APPROVED));
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

    @Override
    public List<MerchantCustomerServiceDTO> toDTOList(List<MerchantCustomerService> list) {
        if (list == null) {
            return List.of();
        }
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
