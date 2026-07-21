package com.pet.qualification.service;

import com.pet.qualification.dto.QualificationDTO;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface QualificationService {
    String OWNER_TYPE_MERCHANT = "merchant";
    String OWNER_TYPE_KEEPER = "keeper";

    String QUAL_TYPE_BUSINESS_LICENSE = "business_license";
    String QUAL_TYPE_KEEPER_CERTIFICATE = "keeper_certificate";

    String STATUS_PENDING = "pending";
    String STATUS_APPROVED = "approved";

    String VISIBILITY_MASKED_PUBLIC = "masked_public";

    QualificationDTO createPending(String ownerType,
                                   Long ownerId,
                                   Long userId,
                                   String qualType,
                                   String title,
                                   String fileUrl,
                                   String summary);

    List<QualificationDTO> listByOwner(String ownerType, Long ownerId, boolean publicView);

    Map<Long, List<QualificationDTO>> listMapByOwnerIds(String ownerType, Set<Long> ownerIds, boolean publicView);

    List<QualificationDTO> listPending();

    QualificationDTO approve(Long id, Long reviewerId);

    QualificationDTO reject(Long id, Long reviewerId, String remark);

    QualificationDTO createOrUpdatePending(String ownerType, Long ownerId, Long userId, String qualType, String title, String fileUrl, String summary);
}
