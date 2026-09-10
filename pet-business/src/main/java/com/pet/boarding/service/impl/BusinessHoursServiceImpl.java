package com.pet.boarding.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.dto.BusinessHoursDTO;
import com.pet.boarding.dto.BusinessHoursUpsertRequestDTO;
import com.pet.boarding.entity.BusinessHours;
import com.pet.boarding.mapper.BusinessHoursMapper;
import com.pet.boarding.service.BusinessHoursService;
import com.pet.boarding.service.MerchantService;
import com.pet.common.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 营业时间服务实现。
 * <p>
 * 负责商家每日营业时间的配置管理。每次 upsert 或 delete 后触发 {@link MerchantService#refreshStoreState}
 * 以便商家店铺的实时营业状态与时间配置同步。
 */
@Service
@Slf4j
public class BusinessHoursServiceImpl implements BusinessHoursService {

    private final BusinessHoursMapper businessHoursMapper;
    private final MerchantService merchantService;

    public BusinessHoursServiceImpl(BusinessHoursMapper businessHoursMapper, MerchantService merchantService) {
        this.businessHoursMapper = businessHoursMapper;
        this.merchantService = merchantService;
    }

    /**
     * 【查询商家营业时间列表】
     *
     * 业务作用：获取指定商家一周七天的营业时间配置。
     * 调用场景：商家在后台查看或编辑营业时间时调用。
     * 调用链：BusinessHoursController → getByMerchantId → BusinessHoursMapper.selectList（按 day_of_week 升序）
     * 数据处理：按 merchant_id 查询，按 day_of_week 升序排列。
     * 状态影响：只读操作。
     */
    @Override
    public List<BusinessHours> getByMerchantId(Long merchantId) {
        log.info("getByMerchantId() 被调用");
        return businessHoursMapper.selectList(
                new LambdaQueryWrapper<BusinessHours>()
                        .eq(BusinessHours::getMerchant_id_wsh, merchantId)
                        .orderByAsc(BusinessHours::getDay_of_week_wsh));
    }

    /**
     * 【新增或更新营业时间】
     *
     * 业务作用：配置或修改某天的营业时间，操作后刷新店铺营业状态。
     * 调用场景：商家在后台编辑营业时间时调用。
     * 调用链：BusinessHoursController → upsert @Transactional → BusinessHoursMapper.insert/updateById → MerchantService.refreshStoreState
     * 数据处理：以 merchantId + day_of_week 为唯一键判断，存在则更新否则插入。无论新增还是更新，均触发店铺状态刷新。
     * 业务规则：upsert 后自动触发店铺营业状态重新计算。
     * 状态影响：新增/更新营业时间记录；触发店铺营业状态可能变化。
     * 事务边界：写入 + 状态刷新在同一事务中。
     */
    @Override
    @Transactional
    public BusinessHours upsert(Long merchantId, BusinessHoursUpsertRequestDTO dto) {
        log.info("upsert() 被调用");
        BusinessHours hours = new BusinessHours();
        hours.setMerchant_id_wsh(merchantId);
        hours.setDay_of_week_wsh(dto.getDay_of_week_wsh());
        hours.setOpen_time_wsh(dto.getOpen_time_wsh());
        hours.setClose_time_wsh(dto.getClose_time_wsh());
        hours.setIs_closed_wsh(dto.getIs_closed_wsh());
        LambdaQueryWrapper<BusinessHours> wrapper = new LambdaQueryWrapper<BusinessHours>()
                .eq(BusinessHours::getMerchant_id_wsh, merchantId)
                .eq(BusinessHours::getDay_of_week_wsh, dto.getDay_of_week_wsh());
        BusinessHours existing = businessHoursMapper.selectOne(wrapper);
        if (existing != null) {
            hours.setId_wsh(existing.getId_wsh());
            businessHoursMapper.updateById(hours);
            merchantService.refreshStoreState(merchantId);
            return hours;
        }
        businessHoursMapper.insert(hours);
        merchantService.refreshStoreState(merchantId);
        return hours;
    }

    /**
     * 【营业时间实体转DTO】
     *
     * 业务作用：将营业时间实体转换为前端展示所需的 DTO。
     * 调用场景：Controller 层返回营业时间信息前调用。
     * 调用链：各查询 Controller → toDTO
     * 数据处理：字段拷贝。
     * 状态影响：只读操作。
     */
    @Override
    public BusinessHoursDTO toDTO(BusinessHours entity) {
        if (entity == null) return null;
        BusinessHoursDTO dto = new BusinessHoursDTO();
        dto.setId_wsh(entity.getId_wsh());
        dto.setMerchant_id_wsh(entity.getMerchant_id_wsh());
        dto.setDay_of_week_wsh(entity.getDay_of_week_wsh());
        dto.setOpen_time_wsh(entity.getOpen_time_wsh());
        dto.setClose_time_wsh(entity.getClose_time_wsh());
        dto.setIs_closed_wsh(entity.getIs_closed_wsh());
        return dto;
    }

    /**
     * 【删除营业时间（按ID）】
     *
     * 业务作用：根据ID删除营业时间配置，删除后刷新店铺营业状态。
     * 调用场景：商家在后台删除某天的营业时间配置时调用。
     * 调用链：BusinessHoursController → delete(id) @Transactional → BusinessHoursMapper.deleteById → MerchantService.refreshStoreState
     * 数据处理：删除后根据被删记录的 merchantId 触发店铺状态刷新。
     * 业务规则：删除当天营业时间后，如果在自动模式下，店铺可能变为关闭状态。
     * 状态影响：删除营业时间记录；触发店铺营业状态刷新。
     * 事务边界：删除 + 状态刷新在同一事务中。
     */
    @Override
    @Transactional
    public void delete(Long id) {
        log.info("delete() 被调用");
        BusinessHours existing = businessHoursMapper.selectById(id);
        businessHoursMapper.deleteById(id);
        if (existing != null && existing.getMerchant_id_wsh() != null) {
            merchantService.refreshStoreState(existing.getMerchant_id_wsh());
        }
    }

    /**
     * 【删除营业时间（按商家+ID，校验归属权）】
     *
     * 业务作用：根据商家ID和营业时间ID删除，校验归属权防止越权操作。
     * 调用场景：商家在后台删除营业时间时调用（带权限校验）。
     * 调用链：BusinessHoursController → delete(merchantId, id) @Transactional → 校验归属权 → delete(id)
     * 数据处理：先校验该记录是否属于指定商家，通过后委托 delete(Long) 执行删除和刷新。
     * 业务规则：仅该商家可以删除自己的营业时间配置。
     * 状态影响：删除营业时间记录；触发店铺营业状态刷新。
     * 事务边界：校验 + 删除 + 状态刷新在同一事务中。
     * 异常情况：记录不存在抛 404；不属于该商家抛 403。
     */
    @Override
    @Transactional
    public void delete(Long merchantId, Long id) {
        log.info("delete(merchantId, id) 被调用");
        BusinessHours existing = businessHoursMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "Business hours not found");
        }
        if (!merchantId.equals(existing.getMerchant_id_wsh())) {
            throw new BusinessException(403, "No permission to delete this business hours record");
        }
        delete(id);
    }
}
