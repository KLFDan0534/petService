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

    @Override
    public List<MerchantCustomerServiceDTO> listMine(Long userId) {
        return toDTOList(mapper.selectList(new LambdaQueryWrapper<MerchantCustomerService>()
                .eq(MerchantCustomerService::getUser_id_wsh, userId)
                .orderByDesc(MerchantCustomerService::getCreated_at_wsh)));
    }

    @Override
    public List<MerchantCustomerServiceDTO> listPendingForMerchant(Long merchantUserId) {
        Merchant merchant = requireMerchantOwner(merchantUserId);
        return toDTOList(mapper.selectList(new LambdaQueryWrapper<MerchantCustomerService>()
                .eq(MerchantCustomerService::getMerchant_id_wsh, merchant.getId_wsh())
                .eq(MerchantCustomerService::getStatus_wsh, STATUS_PENDING)
                .orderByDesc(MerchantCustomerService::getCreated_at_wsh)));
    }

    @Override
    public List<MerchantCustomerServiceDTO> listApprovedForMerchant(Long merchantUserId) {
        Merchant merchant = requireMerchantOwner(merchantUserId);
        return toDTOList(mapper.selectList(new LambdaQueryWrapper<MerchantCustomerService>()
                .eq(MerchantCustomerService::getMerchant_id_wsh, merchant.getId_wsh())
                .eq(MerchantCustomerService::getStatus_wsh, STATUS_APPROVED)
                .orderByDesc(MerchantCustomerService::getReviewed_at_wsh)));
    }

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

    @Override
    public boolean isMerchantCustomerService(Long userId, Long merchantId) {
        return merchantId != null && getApprovedMerchantIds(userId).contains(merchantId);
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

    private MerchantCustomerServiceDTO toDTO(MerchantCustomerService entity) {
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

    private List<MerchantCustomerServiceDTO> toDTOList(List<MerchantCustomerService> list) {
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
