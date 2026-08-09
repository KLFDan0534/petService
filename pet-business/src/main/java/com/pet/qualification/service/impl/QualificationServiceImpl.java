package com.pet.qualification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.qualification.dto.QualificationDTO;
import com.pet.qualification.entity.Qualification;
import com.pet.qualification.mapper.QualificationMapper;
import com.pet.qualification.service.QualificationService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QualificationServiceImpl implements QualificationService {
    private final QualificationMapper qualificationMapper;

    public QualificationServiceImpl(QualificationMapper qualificationMapper) {
        this.qualificationMapper = qualificationMapper;
    }

    /**
     * Creates a pending qualification record. Returns null if no file URL is provided.
     * Sets default title based on qualType and default visibility to masked_public.
     *
     * @param ownerType the owner type
     * @param ownerId   the owner ID
     * @param userId    the submitting user ID
     * @param qualType  the qualification type
     * @param title     display title; auto-generated if null/blank
     * @param fileUrl   URL to the uploaded file
     * @param summary   optional summary
     * @return the created DTO, or null if fileUrl is blank
     */
    @Override
    public QualificationDTO createPending(String ownerType,
                                          Long ownerId,
                                          Long userId,
                                          String qualType,
                                          String title,
                                          String fileUrl,
                                          String summary) {
        if (!StringUtils.hasText(fileUrl)) {
            return null;
        }
        Qualification q = new Qualification();
        q.setOwner_type_wsh(ownerType);
        q.setOwner_id_wsh(ownerId);
        q.setUser_id_wsh(userId);
        q.setQual_type_wsh(qualType);
        q.setTitle_wsh(StringUtils.hasText(title) ? title : defaultTitle(qualType));
        q.setFile_url_wsh(fileUrl);
        q.setSummary_wsh(summary);
        q.setStatus_wsh(STATUS_PENDING);
        q.setVisibility_wsh(VISIBILITY_MASKED_PUBLIC);
        qualificationMapper.insert(q);
        return toDTO(q, false);
    }

    /**
     * Lists qualifications for a given owner, ordered by creation time descending.
     * Returns empty list if ownerType or ownerId is invalid.
     *
     * @param ownerType the owner type
     * @param ownerId   the owner ID
     * @param publicView if true, masks the file URL for public display
     * @return list of qualification DTOs
     */
    @Override
    public List<QualificationDTO> listByOwner(String ownerType, Long ownerId, boolean publicView) {
        if (!StringUtils.hasText(ownerType) || ownerId == null) {
            return List.of();
        }
        return qualificationMapper.selectList(new LambdaQueryWrapper<Qualification>()
                        .eq(Qualification::getOwner_type_wsh, ownerType)
                        .eq(Qualification::getOwner_id_wsh, ownerId)
                        .orderByDesc(Qualification::getCreated_at_wsh))
                .stream()
                .map(q -> toDTO(q, publicView))
                .toList();
    }

    /**
     * Batch-queries qualifications for multiple owners, returning results
     * grouped by owner ID. Returns empty map if inputs are invalid.
     *
     * @param ownerType the owner type
     * @param ownerIds  set of owner IDs to query
     * @param publicView if true, masks file URLs
     * @return map of owner ID to their qualification DTOs
     */
    @Override
    public Map<Long, List<QualificationDTO>> listMapByOwnerIds(String ownerType, Set<Long> ownerIds, boolean publicView) {
        if (!StringUtils.hasText(ownerType) || ownerIds == null || ownerIds.isEmpty()) {
            return Map.of();
        }
        return qualificationMapper.selectList(new LambdaQueryWrapper<Qualification>()
                        .eq(Qualification::getOwner_type_wsh, ownerType)
                        .in(Qualification::getOwner_id_wsh, ownerIds)
                        .orderByDesc(Qualification::getCreated_at_wsh))
                .stream()
                .map(q -> toDTO(q, publicView))
                .collect(Collectors.groupingBy(QualificationDTO::getOwner_id_wsh));
    }

    /**
     * Lists all qualifications in "pending" review status, ordered by creation time descending.
     *
     * @return list of pending qualification DTOs
     */
    @Override
    public List<QualificationDTO> listPending() {
        return qualificationMapper.selectList(
                        new LambdaQueryWrapper<Qualification>()
                                .eq(Qualification::getStatus_wsh, STATUS_PENDING)
                                .orderByDesc(Qualification::getCreated_at_wsh))
                .stream()
                .map(q -> toDTO(q, false))
                .toList();
    }

    /**
     * Approves a pending qualification. Validates that the record exists
     * and is currently in "pending" status before approving.
     *
     * @param id         the qualification ID
     * @param reviewerId the admin reviewer ID
     * @return the approved DTO
     */
    @Override
    public QualificationDTO approve(Long id, Long reviewerId) {
        Qualification q = qualificationMapper.selectById(id);
        if (q == null) {
            throw new BusinessException("资质记录不存在");
        }
        if (!STATUS_PENDING.equals(q.getStatus_wsh())) {
            throw new BusinessException("该资质不在待审核状态");
        }
        q.setStatus_wsh(STATUS_APPROVED);
        q.setReviewer_id_wsh(reviewerId);
        qualificationMapper.updateById(q);
        return toDTO(q, false);
    }

    /**
     * Rejects a pending qualification with a review remark. Validates that
     * the record exists and is in "pending" status before rejecting.
     *
     * @param id         the qualification ID
     * @param reviewerId the admin reviewer ID
     * @param remark     the rejection reason
     * @return the rejected DTO
     */
    @Override
    public QualificationDTO reject(Long id, Long reviewerId, String remark) {
        Qualification q = qualificationMapper.selectById(id);
        if (q == null) {
            throw new BusinessException("资质记录不存在");
        }
        if (!STATUS_PENDING.equals(q.getStatus_wsh())) {
            throw new BusinessException("该资质不在待审核状态");
        }
        q.setStatus_wsh("rejected");
        q.setReviewer_id_wsh(reviewerId);
        q.setReview_remark_wsh(remark);
        qualificationMapper.updateById(q);
        return toDTO(q, false);
    }

    /**
     * Creates a new pending qualification or updates an existing pending one
     * for the same owner and type. If a pending record already exists, only
     * the file_url and summary are updated.
     *
     * @param ownerType the owner type
     * @param ownerId   the owner ID
     * @param userId    the submitting user ID
     * @param qualType  the qualification type
     * @param title     display title
     * @param fileUrl   URL to the uploaded file
     * @param summary   optional summary
     * @return the created or updated DTO, or null if fileUrl is blank
     */
    @Override
    public QualificationDTO createOrUpdatePending(String ownerType,
                                                  Long ownerId,
                                                  Long userId,
                                                  String qualType,
                                                  String title,
                                                  String fileUrl,
                                                  String summary) {
        if (!StringUtils.hasText(fileUrl)) {
            return null;
        }
        Qualification existing = qualificationMapper.selectOne(
                new LambdaQueryWrapper<Qualification>()
                        .eq(Qualification::getOwner_type_wsh, ownerType)
                        .eq(Qualification::getOwner_id_wsh, ownerId)
                        .eq(Qualification::getStatus_wsh, STATUS_PENDING)
                        .last("LIMIT 1"));
        if (existing != null) {
            existing.setFile_url_wsh(fileUrl);
            existing.setSummary_wsh(summary);
            qualificationMapper.updateById(existing);
            return toDTO(existing, false);
        }
        Qualification q = new Qualification();
        q.setOwner_type_wsh(ownerType);
        q.setOwner_id_wsh(ownerId);
        q.setUser_id_wsh(userId);
        q.setQual_type_wsh(qualType);
        q.setTitle_wsh(StringUtils.hasText(title) ? title : defaultTitle(qualType));
        q.setFile_url_wsh(fileUrl);
        q.setSummary_wsh(summary);
        q.setStatus_wsh(STATUS_PENDING);
        q.setVisibility_wsh(VISIBILITY_MASKED_PUBLIC);
        qualificationMapper.insert(q);
        return toDTO(q, false);
    }

    private QualificationDTO toDTO(Qualification q, boolean publicView) {
        QualificationDTO dto = new QualificationDTO();
        dto.setId_wsh(q.getId_wsh());
        dto.setOwner_type_wsh(q.getOwner_type_wsh());
        dto.setOwner_id_wsh(q.getOwner_id_wsh());
        dto.setUser_id_wsh(q.getUser_id_wsh());
        dto.setQual_type_wsh(q.getQual_type_wsh());
        dto.setTitle_wsh(q.getTitle_wsh());
        dto.setFile_url_wsh(publicView ? maskUrl(q.getFile_url_wsh()) : q.getFile_url_wsh());
        dto.setSummary_wsh(q.getSummary_wsh());
        dto.setStatus_wsh(q.getStatus_wsh());
        dto.setVisibility_wsh(q.getVisibility_wsh());
        dto.setCreated_at_wsh(q.getCreated_at_wsh());
        dto.setUpdated_at_wsh(q.getUpdated_at_wsh());
        return dto;
    }

    private String defaultTitle(String qualType) {
        if (QUAL_TYPE_BUSINESS_LICENSE.equals(qualType)) {
            return "营业执照";
        }
        if (QUAL_TYPE_KEEPER_CERTIFICATE.equals(qualType)) {
            return "看护资质";
        }
        return "资质证明";
    }

    private String maskUrl(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return fileUrl;
        }
        return fileUrl;
    }
}
