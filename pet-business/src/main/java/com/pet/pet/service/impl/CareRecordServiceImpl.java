package com.pet.pet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.boarding.service.KeeperAttendanceService;
import com.pet.common.BusinessException;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.pet.dto.CareRecordCreateRequestDTO;
import com.pet.pet.dto.CareRecordDTO;
import com.pet.pet.dto.CareRecordUpdateRequestDTO;
import com.pet.pet.entity.CareRecord;
import com.pet.pet.mapper.CareRecordMapper;
import com.pet.pet.service.CareRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CareRecordServiceImpl implements CareRecordService {

    private final CareRecordMapper careRecordMapper;
    private final OrderMapper orderMapper;
    private final KeeperMapper keeperMapper;
    private final MerchantMapper merchantMapper;
    private final KeeperAttendanceService keeperAttendanceService;

    public CareRecordServiceImpl(CareRecordMapper careRecordMapper,
                                 OrderMapper orderMapper,
                                 KeeperMapper keeperMapper,
                                 MerchantMapper merchantMapper,
                                 KeeperAttendanceService keeperAttendanceService) {
        this.careRecordMapper = careRecordMapper;
        this.orderMapper = orderMapper;
        this.keeperMapper = keeperMapper;
        this.merchantMapper = merchantMapper;
        this.keeperAttendanceService = keeperAttendanceService;
    }

    @Override
    public List<CareRecordDTO> listByOrder(Long orderId) {
        return toDTOList(careRecordMapper.selectList(
                new LambdaQueryWrapper<CareRecord>()
                        .eq(CareRecord::getOrder_id_wsh, orderId)
                        .orderByDesc(CareRecord::getRecord_time_wsh)));
    }

    @Override
    public List<CareRecordDTO> listByOrder(Long actorUserId, boolean admin, Long orderId) {
        PetOrder order = requireOrder(orderId);
        if (!admin) {
            requireReadableAccess(actorUserId, order);
        }
        return listByOrder(orderId);
    }

    @Override
    public CareRecordDTO getById(Long id) {
        CareRecord record = careRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("记录不存在");
        }
        return toDTO(record);
    }

    @Override
    public CareRecordDTO getById(Long actorUserId, boolean admin, Long id) {
        CareRecord record = getByIdRaw(id);
        if (!admin) {
            PetOrder order = requireOrder(record.getOrder_id_wsh());
            requireReadableAccess(actorUserId, order);
        }
        return toDTO(record);
    }

    @Transactional
    @Override
    public CareRecordDTO create(CareRecordCreateRequestDTO request) {
        if (request.getOrder_id_wsh() == null) {
            throw new BusinessException("订单ID不能为空");
        }
        PetOrder order = orderMapper.selectById(request.getOrder_id_wsh());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        CareRecord record = new CareRecord();
        record.setOrder_id_wsh(request.getOrder_id_wsh());
        record.setPet_id_wsh(request.getPet_id_wsh() != null ? request.getPet_id_wsh() : order.getPet_id_wsh());
        record.setKeeper_id_wsh(request.getKeeper_id_wsh() != null ? request.getKeeper_id_wsh() : order.getKeeper_id_wsh());
        record.setType_wsh(request.getType_wsh());
        record.setContent_wsh(request.getContent_wsh());
        record.setImages_wsh(request.getImages_wsh());
        record.setRecord_time_wsh(request.getRecord_time_wsh() != null ? request.getRecord_time_wsh() : LocalDateTime.now());
        careRecordMapper.insert(record);
        return toDTO(record);
    }

    @Transactional
    @Override
    public CareRecordDTO create(Long actorUserId, boolean admin, CareRecordCreateRequestDTO request) {
        if (request.getOrder_id_wsh() == null) {
            throw new BusinessException(400, "order id cannot be empty");
        }
        PetOrder order = orderMapper.selectById(request.getOrder_id_wsh());
        if (order == null) {
            throw new BusinessException(404, "order not found");
        }
        if (!admin) {
            requireWritableAccess(actorUserId, order);
            keeperAttendanceService.requireKeeperOnDuty(order.getKeeper_id_wsh(), order.getMerchant_id_wsh());
        }
        CareRecord record = new CareRecord();
        record.setOrder_id_wsh(request.getOrder_id_wsh());
        record.setPet_id_wsh(request.getPet_id_wsh() != null ? request.getPet_id_wsh() : order.getPet_id_wsh());
        record.setKeeper_id_wsh(request.getKeeper_id_wsh() != null ? request.getKeeper_id_wsh() : order.getKeeper_id_wsh());
        record.setType_wsh(request.getType_wsh());
        record.setContent_wsh(request.getContent_wsh());
        record.setImages_wsh(request.getImages_wsh());
        record.setRecord_time_wsh(request.getRecord_time_wsh() != null ? request.getRecord_time_wsh() : LocalDateTime.now());
        careRecordMapper.insert(record);
        return toDTO(record);
    }

    private void requireWritableAccess(Long actorUserId, PetOrder order) {
        if (!isAssignedKeeperOrMerchant(actorUserId, order)) {
            throw new BusinessException(403, "no permission to maintain care records for this order");
        }
        if (!isOrderServiceWritable(order)) {
            throw new BusinessException(400, "当前订单状态不能维护护理记录");
        }
    }

    private void requireReadableAccess(Long actorUserId, PetOrder order) {
        if (actorUserId == null) {
            throw new BusinessException(401, "please sign in");
        }
        if (actorUserId.equals(order.getOwner_id_wsh())) {
            return;
        }
        if (isAssignedKeeperOrMerchant(actorUserId, order)) {
            return;
        }
        throw new BusinessException(403, "no permission to access care records for this order");
    }

    private boolean isAssignedKeeperOrMerchant(Long actorUserId, PetOrder order) {
        if (actorUserId == null || order == null) {
            return false;
        }
        if (order.getKeeper_id_wsh() != null) {
            Keeper keeper = keeperMapper.selectById(order.getKeeper_id_wsh());
            if (keeper != null && actorUserId.equals(keeper.getUser_id_wsh())) {
                return true;
            }
        }
        if (order.getMerchant_id_wsh() != null) {
            Merchant merchant = merchantMapper.selectById(order.getMerchant_id_wsh());
            if (merchant != null && actorUserId.equals(merchant.getUser_id_wsh())) {
                return true;
            }
        }
        return false;
    }

    private boolean isOrderServiceWritable(PetOrder order) {
        String status = order == null ? null : order.getStatus_wsh();
        return "received".equals(status) || "in_progress".equals(status);
    }

    @Transactional
    @Override
    public CareRecordDTO update(Long id, CareRecordUpdateRequestDTO request) {
        CareRecord existing = getByIdRaw(id);
        applyUpdate(existing, request);
        careRecordMapper.updateById(existing);
        return toDTO(existing);
    }

    @Transactional
    @Override
    public CareRecordDTO update(Long actorUserId, boolean admin, Long id, CareRecordUpdateRequestDTO request) {
        CareRecord existing = getByIdRaw(id);
        if (!admin) {
            PetOrder order = requireOrder(existing.getOrder_id_wsh());
            requireWritableAccess(actorUserId, order);
            keeperAttendanceService.requireKeeperOnDuty(order.getKeeper_id_wsh(), order.getMerchant_id_wsh());
        }
        applyUpdate(existing, request);
        careRecordMapper.updateById(existing);
        return toDTO(existing);
    }

    private void applyUpdate(CareRecord existing, CareRecordUpdateRequestDTO request) {
        if (request.getType_wsh() != null) existing.setType_wsh(request.getType_wsh());
        if (request.getContent_wsh() != null) existing.setContent_wsh(request.getContent_wsh());
        if (request.getImages_wsh() != null) existing.setImages_wsh(request.getImages_wsh());
        if (request.getRecord_time_wsh() != null) existing.setRecord_time_wsh(request.getRecord_time_wsh());
    }

    @Transactional
    @Override
    public void delete(Long id) {
        careRecordMapper.deleteById(id);
    }

    @Transactional
    @Override
    public void delete(Long actorUserId, boolean admin, Long id) {
        CareRecord existing = getByIdRaw(id);
        if (!admin) {
            PetOrder order = requireOrder(existing.getOrder_id_wsh());
            requireWritableAccess(actorUserId, order);
            keeperAttendanceService.requireKeeperOnDuty(order.getKeeper_id_wsh(), order.getMerchant_id_wsh());
        }
        careRecordMapper.deleteById(id);
    }

    private PetOrder requireOrder(Long orderId) {
        if (orderId == null) {
            throw new BusinessException(400, "order id cannot be empty");
        }
        PetOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "order not found");
        }
        return order;
    }

    private CareRecord getByIdRaw(Long id) {
        CareRecord record = careRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("记录不存在");
        }
        return record;
    }

    private CareRecordDTO toDTO(CareRecord record) {
        if (record == null) return null;
        CareRecordDTO dto = new CareRecordDTO();
        dto.setId_wsh(record.getId_wsh());
        dto.setOrder_id_wsh(record.getOrder_id_wsh());
        dto.setPet_id_wsh(record.getPet_id_wsh());
        dto.setKeeper_id_wsh(record.getKeeper_id_wsh());
        dto.setType_wsh(record.getType_wsh());
        dto.setContent_wsh(record.getContent_wsh());
        dto.setImages_wsh(record.getImages_wsh());
        dto.setRecord_time_wsh(record.getRecord_time_wsh());
        dto.setCreated_at_wsh(record.getCreated_at_wsh());
        return dto;
    }

    private List<CareRecordDTO> toDTOList(List<CareRecord> list) {
        if (list == null) return List.of();
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }
}
