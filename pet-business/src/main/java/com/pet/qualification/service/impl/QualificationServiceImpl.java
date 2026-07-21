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
