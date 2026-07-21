package com.pet.customer.service;

import com.pet.customer.dto.MerchantCustomerServiceApplyRequestDTO;
import com.pet.customer.dto.MerchantCustomerServiceDTO;
import com.pet.customer.dto.MerchantCustomerServiceReviewRequestDTO;

import java.util.List;
import java.util.Set;

public interface MerchantCustomerServiceService {
    String STATUS_PENDING = "pending";
    String STATUS_APPROVED = "approved";
    String STATUS_REJECTED = "rejected";
    String STATUS_RESIGNED = "resigned";
    String STATUS_TERMINATED = "terminated";

    MerchantCustomerServiceDTO apply(Long userId, MerchantCustomerServiceApplyRequestDTO request);

    List<MerchantCustomerServiceDTO> listMine(Long userId);

    List<MerchantCustomerServiceDTO> listPendingForMerchant(Long merchantUserId);

    List<MerchantCustomerServiceDTO> listApprovedForMerchant(Long merchantUserId);

    MerchantCustomerServiceDTO approve(Long id, Long merchantUserId, MerchantCustomerServiceReviewRequestDTO request);

    MerchantCustomerServiceDTO reject(Long id, Long merchantUserId, MerchantCustomerServiceReviewRequestDTO request);

    MerchantCustomerServiceDTO resign(Long id, Long userId);

    MerchantCustomerServiceDTO terminateByMerchant(Long id, Long merchantUserId);

    Set<Long> getApprovedMerchantIds(Long userId);

    boolean isMerchantCustomerService(Long userId, Long merchantId);
}
