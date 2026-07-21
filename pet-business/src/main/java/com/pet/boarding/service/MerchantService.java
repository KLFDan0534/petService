package com.pet.boarding.service;

import com.pet.boarding.dto.MerchantCreateRequestDTO;
import com.pet.boarding.dto.MerchantDTO;
import com.pet.boarding.dto.MerchantUpdateRequestDTO;
import com.pet.boarding.entity.Merchant;

import java.util.Collection;
import java.util.List;

public interface MerchantService {
    List<Merchant> listAll();

    Merchant getById(Long id);

    List<Merchant> listByIds(Collection<Long> ids);

    List<MerchantDTO> searchNearby(double lat, double lng, double radius);

    Merchant findByUserId(Long userId);

    Merchant create(MerchantCreateRequestDTO dto, Long userId);

    Merchant update(Long id, MerchantUpdateRequestDTO dto);

    MerchantDTO toDTO(Merchant entity);

    void approve(Long id);

    void reject(Long id);

    Merchant updateStoreMode(Long id, Integer storeMode);

    void refreshStoreState(Long merchantId);

    void refreshAllStoreStates();

    boolean isOwner(Long merchantId, Long userId);
}
