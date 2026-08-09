package com.pet.boarding.service;

import com.pet.boarding.dto.BusinessHoursDTO;
import com.pet.boarding.dto.BusinessHoursUpsertRequestDTO;
import com.pet.boarding.entity.BusinessHours;
import java.util.List;

/**
 * 商家营业时间管理服务接口。
 * <p>
 * 管理商家的每日营业时间配置，支持一周七天分别设置营业时段和休息标记。
 * 营业时间变更后自动触发店铺营业状态刷新（{@link MerchantService#refreshStoreState}），
 * 确保店铺的实时营业状态与营业时间配置保持同步。
 * 商家可在自动模式（根据营业时间自动开关店）或手动模式下工作。
 */
public interface BusinessHoursService {

    /**
     * 【查询商家营业时间列表】
     *
     * 业务作用：获取指定商家一周七天的营业时间配置。
     * 调用场景：商家在后台查看或编辑营业时间时调用。
     * 调用链：BusinessHoursController → getByMerchantId → BusinessHoursMapper.selectList
     * 数据处理：按 merchant_id 查询，按 day_of_week 升序排列。
     * 状态影响：只读操作。
     *
     * @param merchantId 商家ID
     * @return 营业时间列表
     */
    List<BusinessHours> getByMerchantId(Long merchantId);

    /**
     * 【新增或更新营业时间】
     *
     * 业务作用：配置或修改某天的营业时间（含开门/关门时间、休息标记）。
     * 调用场景：商家在后台编辑营业时间时调用。
     * 调用链：BusinessHoursController → upsert @Transactional → BusinessHoursMapper.insert/updateById → MerchantService.refreshStoreState
     * 数据处理：如果该商家该天已有记录则覆盖更新，否则新增。操作完成后触发店铺状态刷新。
     * 业务规则：upsert 后自动触发店铺营业状态重新计算。
     * 状态影响：新增/更新营业时间记录；触发店铺营业状态可能变化。
     *
     * @param merchantId 商家ID
     * @param dto        营业时间请求DTO（包含 day_of_week, open_time, close_time, is_closed）
     * @return 更新后的营业时间
     */
    BusinessHours upsert(Long merchantId, BusinessHoursUpsertRequestDTO dto);

    /**
     * 【删除营业时间（按ID）】
     *
     * 业务作用：根据ID删除营业时间配置，删除后触发店铺状态刷新。
     * 调用场景：商家在后台删除某天的营业时间配置时调用。
     * 调用链：BusinessHoursController → delete(id) → BusinessHoursMapper.deleteById → MerchantService.refreshStoreState
     * 数据处理：删除后根据被删记录的 merchantId 触发店铺状态刷新。
     * 业务规则：删除当天营业时间后店铺可能变为关闭状态。
     * 状态影响：删除营业时间记录；店铺营业状态可能变为关闭。
     *
     * @param id 营业时间ID
     */
    void delete(Long id);

    /**
     * 【删除营业时间（按商家+ID，校验归属权）】
     *
     * 业务作用：根据商家ID和营业时间ID删除，校验归属权防止越权。
     * 调用场景：商家在后台删除营业时间时调用，带权限校验。
     * 调用链：BusinessHoursController → delete(merchantId, id) → 校验归属权 → delete(id)
     * 数据处理：先校验该记录是否属于指定商家，通过后调用 delete(id) 执行删除。
     * 业务规则：仅该商家可以删除自己的营业时间配置。
     * 状态影响：删除营业时间记录；店铺营业状态可能变化。
     * 异常情况：营业时间不存在抛 404；不属于该商家抛 403。
     *
     * @param merchantId 商家ID（用于校验归属权）
     * @param id         营业时间ID
     * @throws BusinessException 如果营业时间不存在或不属于该商家
     */
    void delete(Long merchantId, Long id);

    /**
     * 【营业时间实体转DTO】
     *
     * 业务作用：将营业时间实体转换为前端展示所需的 DTO。
     * 调用场景：Controller 层返回营业时间信息前调用。
     * 调用链：各查询 Controller → toDTO
     * 数据处理：字段拷贝。
     * 状态影响：只读操作。
     *
     * @param entity 营业时间实体
     * @return 营业时间DTO，入参为 null 时返回 null
     */
    BusinessHoursDTO toDTO(BusinessHours entity);
}

