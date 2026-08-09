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

    /**
     * Creates a qualification record in "pending" status for review.
     * Returns null if the file URL is empty.
     *
     * @param ownerType the owner type ("merchant" or "keeper")
     * @param ownerId   the owner ID
     * @param userId    the submitting user ID
     * @param qualType  the qualification type
     * @param title     display title; defaults to a type-based label if null
     * @param fileUrl   URL to the uploaded qualification file
     * @param summary   optional summary text
     * @return the created DTO, or null if fileUrl is blank
     */
    QualificationDTO createPending(String ownerType,
                                   Long ownerId,
                                   Long userId,
                                   String qualType,
                                   String title,
                                   String fileUrl,
                                   String summary);

    /**
     * Lists qualification records for a given owner.
     * When publicView is true, sensitive data (file URL) is masked.
     *
     * @param ownerType the owner type
     * @param ownerId   the owner ID
     * @param publicView whether to mask sensitive fields for public display
     * @return list of qualification DTOs
     */
    List<QualificationDTO> listByOwner(String ownerType, Long ownerId, boolean publicView);

    /**
     * Batch-queries qualifications for multiple owners, returned as a map
     * keyed by owner ID. Useful for display on listing pages.
     *
     * @param ownerType the owner type
     * @param ownerIds  set of owner IDs
     * @param publicView whether to mask sensitive fields
     * @return map of owner ID to their qualification DTO list
     */
    Map<Long, List<QualificationDTO>> listMapByOwnerIds(String ownerType, Set<Long> ownerIds, boolean publicView);

    /**
     * Lists all qualifications currently in "pending" review status.
     *
     * @return list of pending qualification DTOs
     */
    List<QualificationDTO> listPending();

    /**
     * Approves a pending qualification record.
     * Throws an exception if the record does not exist or is not in pending status.
     *
     * @param id         the qualification ID
     * @param reviewerId the admin reviewer ID
     * @return the approved DTO
     */
    QualificationDTO approve(Long id, Long reviewerId);

    /**
     * Rejects a pending qualification record with a review remark.
     * Throws an exception if the record does not exist or is not in pending status.
     *
     * @param id         the qualification ID
     * @param reviewerId the admin reviewer ID
     * @param remark     the rejection reason
     * @return the rejected DTO
     */
    QualificationDTO reject(Long id, Long reviewerId, String remark);

    /**
     * Creates a new pending qualification or updates the file/summary of an
     * existing pending qualification for the same owner and type.
     * Returns null if the file URL is empty.
     *
     * @param ownerType the owner type
     * @param ownerId   the owner ID
     * @param userId    the submitting user ID
     * @param qualType  the qualification type
     * @param title     display title
     * @param fileUrl   URL to the uploaded qualification file
     * @param summary   optional summary text
     * @return the created or updated DTO, or null if fileUrl is blank
     */
    QualificationDTO createOrUpdatePending(String ownerType, Long ownerId, Long userId, String qualType, String title, String fileUrl, String summary);
}
