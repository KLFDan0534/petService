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
     * 异常情况：依赖缺失时由 Spring 抛出 Bean 创建异常。
     * 注意事项：依赖为 final 字段。
     */
    public QualificationServiceImpl(QualificationMapper qualificationMapper) {
        this.qualificationMapper = qualificationMapper;
    }

    /**
     * 创建一条待审核的资质记录。若未提供文件地址则返回 null。
     * 根据资质类型设置默认标题，默认可见性为 masked_public（脱敏公开）。
     *
     * @param ownerType 归属方类型
     * @param ownerId   归属方 ID
     * @param userId    提交用户的 ID
     * @param qualType  资质类型
     * @param title     展示标题；为空时自动生成
     * @param fileUrl   上传文件的地址
     * @param summary   可选说明
     * @return 创建后的 DTO；若 fileUrl 为空则返回 null
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
     * 查询指定归属方的资质列表，按创建时间倒序排列。
     * 若 ownerType 或 ownerId 无效则返回空列表。
     *
     * @param ownerType  归属方类型
     * @param ownerId    归属方 ID
     * @param publicView 为 true 时对公开展示的文件地址做脱敏
     * @return 资质 DTO 列表
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
     * 批量查询多个归属方的资质，结果按归属方 ID 分组返回。
     * 若入参无效则返回空 Map。
     *
     * @param ownerType  归属方类型
     * @param ownerIds   待查询的归属方 ID 集合
     * @param publicView 为 true 时对文件地址做脱敏
     * @return 归属方 ID 到其资质 DTO 列表的映射
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
     * 查询所有处于"待审核"状态的资质，按创建时间倒序排列。
     *
     * @return 待审核资质的 DTO 列表
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
     * 审核通过一条待审核的资质。会校验记录是否存在且当前处于"待审核"状态。
     *
     * @param id         资质 ID
     * @param reviewerId 审核管理员 ID
     * @return 审核通过后的 DTO
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
     * 驳回一条待审核的资质，并记录审核意见。会校验记录是否存在且当前处于"待审核"状态。
     *
     * @param id         资质 ID
     * @param reviewerId 审核管理员 ID
     * @param remark     驳回原因
     * @return 驳回后的 DTO
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
     * 为同一归属方、同一资质类型创建新的待审核资质，或更新已有的待审核记录。
     * 若已存在待审核记录，则仅更新文件地址与说明。
     *
     * @param ownerType 归属方类型
     * @param ownerId   归属方 ID
     * @param userId    提交用户的 ID
     * @param qualType  资质类型
     * @param title     展示标题
     * @param fileUrl   上传文件的地址
     * @param summary   可选说明
     * @return 创建或更新后的 DTO；若 fileUrl 为空则返回 null
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
     * 业务规则：publicView=true 时文件地址走 maskUrl 处理。
     * 异常情况：入参为 null 时抛 NullPointerException，未判空。
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
     * 业务规则：按常量 QUAL_TYPE_BUSINESS_LICENSE 与 QUAL_TYPE_KEEPER_CERTIFICATE 精确匹配。
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
     * 数据处理：空值原样返回，非空值也原样返回（当前实现未做任何脱敏）。
     * 业务规则：无实际脱敏规则。
     * 注意事项：方法名为 maskUrl 但实现直接返回原地址，公开接口实际上会暴露资质原图 URL，与 visibility_wsh=masked_public 的语义不一致，属待实现/遗留问题。
     */
    private String maskUrl(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return fileUrl;
        }
        return fileUrl;
    }
}
