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

    /**
     * 【业务名称】按订单查询护理记录（实现-无权限）
     * 业务作用：根据订单 ID 查询全部护理记录，按记录时间倒序。
     * 调用场景：内部、管理员查询。
     * 调用链：listByOrder(Long) → CareRecordMapper.selectList() → toDTOList()。
     * 数据处理：按 order_id 精确匹配，按 record_time 倒序。
     * 业务规则：无权限校验。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     */
    @Override
    public List<CareRecordDTO> listByOrder(Long orderId) {
        return toDTOList(careRecordMapper.selectList(
                new LambdaQueryWrapper<CareRecord>()
                        .eq(CareRecord::getOrder_id_wsh, orderId)
                        .orderByDesc(CareRecord::getRecord_time_wsh)));
    }

    /**
     * 【业务名称】按订单查询护理记录（实现-带权限）
     * 业务作用：查询护理记录，非管理员时校验用户可读权限。
     * 调用场景：用户查看订单护理记录。
     * 调用链：listByOrder(Long, boolean, Long) → requireOrder() → requireReadableAccess() → listByOrder(orderId)。
     * 数据处理：委派给无权限版本。
     * 业务规则：非管理员时，宠物主人、分配看护者或商家可读取。
     * 状态影响：无。
     * 异常情况：无权限时抛 BusinessException(403)。
     * 注意事项：管理员跳过权限校验。
     */
    @Override
    public List<CareRecordDTO> listByOrder(Long actorUserId, boolean admin, Long orderId) {
        PetOrder order = requireOrder(orderId);
        if (!admin) {
            requireReadableAccess(actorUserId, order);
        }
        return listByOrder(orderId);
    }

    /**
     * 【业务名称】护理记录详情查询（实现-无权限）
     * 业务作用：根据 ID 查询单条护理记录。
     * 调用场景：内部调用。
     * 调用链：getById(Long) → CareRecordMapper.selectById() → toDTO()。
     * 数据处理：主键查询。
     * 业务规则：不存在时抛异常。
     * 状态影响：无。
     * 异常情况：不存在时抛出 BusinessException("记录不存在")。
     * 注意事项：无。
     */
    @Override
    public CareRecordDTO getById(Long id) {
        CareRecord record = careRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("记录不存在");
        }
        return toDTO(record);
    }

    /**
     * 【业务名称】护理记录详情查询（实现-带权限）
     * 业务作用：根据 ID 查询单条护理记录，非管理员时校验权限。
     * 调用场景：用户查看护理记录详情。
     * 调用链：getById(Long, boolean, Long) → getByIdRaw() → requireReadableAccess() → toDTO()。
     * 数据处理：主键查询，权限校验。
     * 业务规则：非管理员时需满足可读权限（主人、看护者或商家）。
     * 状态影响：无。
     * 异常情况：无权限时抛 BusinessException(403)。
     * 注意事项：管理员跳过权限校验。
     */
    @Override
    public CareRecordDTO getById(Long actorUserId, boolean admin, Long id) {
        CareRecord record = getByIdRaw(id);
        if (!admin) {
            PetOrder order = requireOrder(record.getOrder_id_wsh());
            requireReadableAccess(actorUserId, order);
        }
        return toDTO(record);
    }

    /**
     * 【业务名称】创建护理记录（实现-无权限）
     * 业务作用：创建护理记录，自动从订单继承宠物 ID 和看护者 ID。
     * 调用场景：内部、管理员创建。
     * 调用链：create(CareRecordCreateRequestDTO) → 校验订单 → 构造 → CareRecordMapper.insert() → toDTO()。
     * 数据处理：订单 ID 必填；宠物 ID 和看护者 ID 优先取请求，未指定时从订单继承；记录时间默认当前时间。
     * 业务规则：无权限校验。
     * 状态影响：新增一条护理记录。
     * 异常情况：订单 ID 为空抛 BusinessException；订单不存在抛 BusinessException。
     * 注意事项：无权限校验。
     */
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

    /**
     * 【业务名称】创建护理记录（实现-带权限与考勤）
     * 业务作用：创建护理记录，非管理员时校验权限和考勤状态。
     * 调用场景：看护者提交日常护理记录。
     * 调用链：create(Long, boolean, CareRecordCreateRequestDTO) → 校验订单 → requireWritableAccess() → requireKeeperOnDuty() → 插入 → toDTO()。
     * 数据处理：同无权限版本。
     * 业务规则：非管理员时仅分配看护者或商家可创建；看护者需处于值班状态。
     * 状态影响：新增一条护理记录。
     * 异常情况：无权限抛 BusinessException(403)；考勤不通过抛异常。
     * 注意事项：管理员跳过权限和考勤校验。
     */
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

    /**
     * 【业务名称】更新护理记录（实现-无权限）
     * 业务作用：更新护理记录，仅更新非 null 字段。
     * 调用场景：内部、管理员更新。
     * 调用链：update(Long, CareRecordUpdateRequestDTO) → getByIdRaw() → applyUpdate() → updateById() → toDTO()。
     * 数据处理：仅更新类型、内容、图片、记录时间中非 null 字段。
     * 业务规则：无权限校验。
     * 状态影响：更新护理记录。
     * 异常情况：记录不存在时抛 BusinessException。
     * 注意事项：无。
     */
    @Transactional
    @Override
    public CareRecordDTO update(Long id, CareRecordUpdateRequestDTO request) {
        CareRecord existing = getByIdRaw(id);
        applyUpdate(existing, request);
        careRecordMapper.updateById(existing);
        return toDTO(existing);
    }

    /**
     * 【业务名称】更新护理记录（实现-带权限与考勤）
     * 业务作用：更新护理记录，非管理员时校验权限和考勤状态。
     * 调用场景：看护者修改护理记录。
     * 调用链：update(Long, boolean, Long, CareRecordUpdateRequestDTO) → getByIdRaw() → requireWritableAccess() → requireKeeperOnDuty() → applyUpdate() → updateById() → toDTO()。
     * 数据处理：同无权限版本。
     * 业务规则：非管理员时仅分配看护者或商家可更新，且需处于值班状态。
     * 状态影响：更新护理记录。
     * 异常情况：无权限抛 BusinessException(403)。
     * 注意事项：管理员跳过权限和考勤校验。
     */
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

    /**
     * 【业务名称】删除护理记录（实现-无权限）
     * 业务作用：删除护理记录。
     * 调用场景：内部、管理员删除。
     * 调用链：delete(Long) → CareRecordMapper.deleteById()。
     * 数据处理：物理删除。
     * 业务规则：无权限校验。
     * 状态影响：删除护理记录。
     * 异常情况：无。
     * 注意事项：无。
     */
    @Transactional
    @Override
    public void delete(Long id) {
        careRecordMapper.deleteById(id);
    }

    /**
     * 【业务名称】删除护理记录（实现-带权限与考勤）
     * 业务作用：删除护理记录，非管理员时校验权限和考勤状态。
     * 调用场景：看护者删除护理记录。
     * 调用链：delete(Long, boolean, Long) → getByIdRaw() → requireWritableAccess() → requireKeeperOnDuty() → deleteById()。
     * 数据处理：物理删除。
     * 业务规则：非管理员时仅分配看护者或商家可删除，且需处于值班状态。
     * 状态影响：删除护理记录。
     * 异常情况：无权限抛 BusinessException(403)。
     * 注意事项：管理员跳过权限校验。
     */
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
