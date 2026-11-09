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

    /**
     * 【构造方法】注入资质 Mapper
     *
     * 业务作用：由 Spring 注入 QualificationMapper，供资质读写使用。
     *
     * 调用场景：Spring 容器启动创建该 Service Bean 时调用。
     *
     * 调用链：Spring 容器 → 本构造方法 → qualificationMapper 赋值。
     *
     * 数据处理：仅字段赋值。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：依赖缺失时由 Spring 抛出 Bean 创建异常。
     *
     * 注意事项：依赖为 final 字段。
     */
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
        // 标题为空时按资质类型生成默认标题
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
        // 仅待审核状态可审批
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
        // 仅待审核状态可驳回
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
        // 已有待审核记录则原地更新文件与说明
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

    /**
     * 【资质实体转DTO】按可见性参数决定是否脱敏文件地址
     *
     * 业务作用：统一把 Qualification 实体转换为对外 DTO，publicView 时对 file_url_wsh 调用 maskUrl。
     *
     * 调用场景：createPending、listByOwner、listMapByOwnerIds、listPending、approve、reject、createOrUpdatePending 中调用。
     *
     * 调用链：上述方法 → 本方法 → maskUrl。
     *
     * 数据处理：拷贝 id_wsh、owner_type_wsh、owner_id_wsh、user_id_wsh、qual_type_wsh、title_wsh、file_url_wsh、summary_wsh、status_wsh、visibility_wsh、created_at_wsh、updated_at_wsh；不返回 reviewer_id_wsh 与 review_remark_wsh。
     *
     * 业务规则：publicView=true 时文件地址走 maskUrl 处理。
     *
     * 状态影响：无。
     *
     * 异常情况：入参为 null 时抛 NullPointerException，未判空。
     *
     * 注意事项：DTO 中不含审核人、审核备注与驳回原因，后台审核页无法从该 DTO 读取审核意见。
     */
    private QualificationDTO toDTO(Qualification q, boolean publicView) {
        QualificationDTO dto = new QualificationDTO();
        dto.setId_wsh(q.getId_wsh());
        dto.setOwner_type_wsh(q.getOwner_type_wsh());
        dto.setOwner_id_wsh(q.getOwner_id_wsh());
        dto.setUser_id_wsh(q.getUser_id_wsh());
        dto.setQual_type_wsh(q.getQual_type_wsh());
        dto.setTitle_wsh(q.getTitle_wsh());
        // 公开视图走脱敏，内部视图返回原地址
        dto.setFile_url_wsh(publicView ? maskUrl(q.getFile_url_wsh()) : q.getFile_url_wsh());
        dto.setSummary_wsh(q.getSummary_wsh());
        dto.setStatus_wsh(q.getStatus_wsh());
        dto.setVisibility_wsh(q.getVisibility_wsh());
        dto.setCreated_at_wsh(q.getCreated_at_wsh());
        dto.setUpdated_at_wsh(q.getUpdated_at_wsh());
        return dto;
    }

    /**
     * 【生成默认标题】按资质类型给出中文标题
     *
     * 业务作用：提交资质时未填标题则按类型自动生成展示标题。
     *
     * 调用场景：createPending 与 createOrUpdatePending 在 title 为空时调用。
     *
     * 调用链：createPending / createOrUpdatePending → 本方法。
     *
     * 数据处理：营业执照类型返回「营业执照」，看护资质类型返回「看护资质」，其它返回「资质证明」。
     *
     * 业务规则：按常量 QUAL_TYPE_BUSINESS_LICENSE 与 QUAL_TYPE_KEEPER_CERTIFICATE 精确匹配。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：新增资质类型时需同步扩充本方法，否则一律落到「资质证明」。
     */
    private String defaultTitle(String qualType) {
        if (QUAL_TYPE_BUSINESS_LICENSE.equals(qualType)) {
            return "营业执照";
        }
        if (QUAL_TYPE_KEEPER_CERTIFICATE.equals(qualType)) {
            return "看护资质";
        }
        return "资质证明";
    }

    /**
     * 【脱敏文件地址】对公开视图下的资质文件地址做处理
     *
     * 业务作用：名义上用于公开视图隐藏原始文件地址。
     *
     * 调用场景：toDTO 在 publicView=true 时调用。
     *
     * 调用链：toDTO → 本方法。
     *
     * 数据处理：空值原样返回，非空值也原样返回（当前实现未做任何脱敏）。
     *
     * 业务规则：无实际脱敏规则。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：方法名为 maskUrl 但实现直接返回原地址，公开接口实际上会暴露资质原图 URL，与 visibility_wsh=masked_public 的语义不一致，属待实现/遗留问题。
     */
    private String maskUrl(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return fileUrl;
        }
        return fileUrl;
    }
}
