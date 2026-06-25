package com.pet.pet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.pet.entity.CareRecord;
import com.pet.pet.mapper.CareRecordMapper;
import com.pet.pet.service.CareRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 护理记录服务实现类
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Service
@Slf4j
public class CareRecordServiceImpl implements CareRecordService {

    private final CareRecordMapper careRecordMapper;
    private final OrderMapper orderMapper;

    public CareRecordServiceImpl(CareRecordMapper careRecordMapper, OrderMapper orderMapper) {
        this.careRecordMapper = careRecordMapper;
        this.orderMapper = orderMapper;
    }

    /**
     * 根据订单ID获取护理记录列表
     * @param orderId 订单ID
     * @return 护理记录列表
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Override
    public List<CareRecord> listByOrder(Long orderId) {
        return careRecordMapper.selectList(
                new LambdaQueryWrapper<CareRecord>()
                        .eq(CareRecord::getOrder_id_wsh, orderId)
                        .orderByDesc(CareRecord::getRecord_time_wsh));
    }

    /**
     * 根据ID获取护理记录
     * @param id 记录ID
     * @return 护理记录实体
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Override
    public CareRecord getById(Long id) {
        CareRecord record = careRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("记录不存在");
        }
        return record;
    }

    /**
     * 创建护理记录
     * @param record 护理记录实体
     * @return 创建后的护理记录
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Transactional
    @Override
    public CareRecord create(CareRecord record) {
        if (record.getOrder_id_wsh() == null) {
            throw new BusinessException("订单ID不能为空");
        }
        PetOrder order = orderMapper.selectById(record.getOrder_id_wsh());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (record.getPet_id_wsh() == null) {
            record.setPet_id_wsh(order.getPet_id_wsh());
        }
        if (record.getKeeper_id_wsh() == null) {
            record.setKeeper_id_wsh(order.getKeeper_id_wsh());
        }
        if (record.getRecord_time_wsh() == null) {
            record.setRecord_time_wsh(LocalDateTime.now());
        }
        careRecordMapper.insert(record);
        return record;
    }

    /**
     * 更新护理记录
     * @param id 记录ID
     * @param record 护理记录实体
     * @return 更新后的护理记录
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Transactional
    @Override
    public CareRecord update(Long id, CareRecord record) {
        CareRecord existing = getById(id);
        if (record.getType_wsh() != null) existing.setType_wsh(record.getType_wsh());
        if (record.getContent_wsh() != null) existing.setContent_wsh(record.getContent_wsh());
        if (record.getImages_wsh() != null) existing.setImages_wsh(record.getImages_wsh());
        if (record.getRecord_time_wsh() != null) existing.setRecord_time_wsh(record.getRecord_time_wsh());
        careRecordMapper.updateById(existing);
        return existing;
    }

    /**
     * 删除护理记录
     * @param id 记录ID
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Transactional
    @Override
    public void delete(Long id) {
        careRecordMapper.deleteById(id);
    }
}
