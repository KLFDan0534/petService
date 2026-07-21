package com.pet.boarding.service;

import com.pet.boarding.dto.KeeperLeaveCreateRequestDTO;
import com.pet.boarding.dto.KeeperLeaveDTO;

import java.time.LocalDate;
import java.util.List;

public interface KeeperLeaveService {
    List<KeeperLeaveDTO> listByMerchant(Long merchantUserId);

    KeeperLeaveDTO createByMerchant(Long merchantUserId, KeeperLeaveCreateRequestDTO request);

    void deleteByMerchant(Long merchantUserId, Long id);

    boolean isKeeperOnLeave(Long keeperId, LocalDate date);

    boolean hasLeaveOverlap(Long keeperId, LocalDate startDate, LocalDate endExclusive);

    void requireKeeperAvailable(Long keeperId, LocalDate startDate, LocalDate endExclusive);
}
