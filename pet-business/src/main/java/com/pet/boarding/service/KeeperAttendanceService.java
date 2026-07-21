package com.pet.boarding.service;

import com.pet.boarding.dto.AttendanceCheckRequestDTO;
import com.pet.boarding.dto.KeeperAttendanceDTO;

import java.util.List;

public interface KeeperAttendanceService {
    KeeperAttendanceDTO checkIn(Long userId, AttendanceCheckRequestDTO request);

    KeeperAttendanceDTO checkOut(Long userId, AttendanceCheckRequestDTO request);

    KeeperAttendanceDTO current(Long userId);

    List<KeeperAttendanceDTO> today(Long userId);

    List<KeeperAttendanceDTO> listMerchantToday(Long merchantUserId);

    boolean hasActiveShift(Long keeperId, Long merchantId);

    void requireKeeperOnDuty(Long keeperId, Long merchantId);
}
