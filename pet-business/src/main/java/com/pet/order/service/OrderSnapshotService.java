package com.pet.order.service;

import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.entity.ServiceItem;
import com.pet.order.dto.OrderCreateRequestDTO;
import com.pet.order.dto.OrderSnapshotDTO;
import com.pet.order.entity.OrderSnapshot;
import com.pet.order.entity.PetOrder;
import com.pet.pet.entity.Pet;
import com.pet.system.entity.User;

import java.util.Map;
import java.util.Set;

public interface OrderSnapshotService {
    OrderSnapshot createForOrder(PetOrder order,
                                 User owner,
                                 Pet pet,
                                 Merchant merchant,
                                 Keeper keeper,
                                 ServiceItem service,
                                 OrderCreateRequestDTO request);

    OrderSnapshotDTO getByOrderId(Long orderId);

    Map<Long, OrderSnapshotDTO> findDTOMapByOrderIds(Set<Long> orderIds);
}
