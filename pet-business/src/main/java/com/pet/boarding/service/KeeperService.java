package com.pet.boarding.service;

import com.pet.boarding.dto.KeeperCreateRequestDTO;
import com.pet.boarding.dto.KeeperUpdateRequestDTO;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.vo.KeeperVO;

import java.util.Collection;
import java.util.List;

public interface KeeperService {
    List<Keeper> listAll();

    List<Keeper> listPending();

    Keeper getById(Long id);

    List<Keeper> listByIds(Collection<Long> ids);

    List<Keeper> findByMerchantId(Long merchantId);

    List<KeeperVO> searchNearby(double lat, double lng, double radius);

    Keeper create(KeeperCreateRequestDTO dto, Long userId);

    Keeper update(Long id, KeeperUpdateRequestDTO dto);

    void delete(Long id);

    void resign(Long id, Long userId);

    void terminateByMerchant(Long id, Long merchantUserId);

    void approve(Long id);

    void reject(Long id);

    KeeperVO toDTO(Keeper entity);

    void setOnlineStatus(Long id, int status);

    void syncMerchantStoreStatus(Long merchantId, boolean storeOpen);

    Keeper findByUserId(Long userId);

    List<Keeper> listPendingByMerchant(Long merchantUserId);

    void approveByMerchant(Long id, Long merchantUserId);

    void rejectByMerchant(Long id, Long merchantUserId);
}
