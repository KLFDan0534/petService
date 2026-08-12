package com.pet.boarding.service;

import com.pet.boarding.dto.ServiceAvailabilityVO;

import java.time.LocalDate;

/**
 * U2: 服务动态可预约性查询服务。
 *
 * <p>以营业时间配置为真相源（复用 {@link BusinessHoursTargetResolver}），
 * 输出按天拆分的营业窗口与起始槽位，供前端预约流程展示。
 * 可选传入看护员 ID 时叠加看护员维度（归属、状态、资质、请假、容量）。
 * 本服务只读，不产生任何写操作。
 */
public interface ServiceAvailabilityService {

    /**
     * 查询服务的动态可预约性。
     *
     * @param serviceId 服务ID
     * @param from      起始日期（包含，不早于今天）
     * @param to        结束日期（包含，与 from 跨度不超过 31 天）
     * @param keeperId  可选看护员ID；为空时不叠加看护员维度
     * @return 按天拆分的可预约性视图
     */
    ServiceAvailabilityVO getAvailability(Long serviceId, LocalDate from, LocalDate to, Long keeperId);
}