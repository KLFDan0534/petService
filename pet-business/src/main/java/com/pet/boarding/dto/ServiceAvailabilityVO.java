package com.pet.boarding.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * U2: 服务动态可预约性视图对象。
 *
 * <p>面向用户端的只读查询结果：
 * <ul>
 *   <li>{@code schedule_source_wsh}：营业窗口来源，{@code business_hours}（营业时间配置）
 *       或 {@code legacy_unrestricted}（无配置时的兼容兜底语义）。</li>
 *   <li>{@code days_wsh}：按天拆分的可预约性，含窗口与槽位列表。</li>
 * </ul>
 */
@Getter
@Setter
public class ServiceAvailabilityVO {

    /** 服务ID */
    private Long service_id_wsh;

    /** 服务所属商家ID */
    private Long merchant_id_wsh;

    /** 服务版本（与详情页一致，用于下单快照校验） */
    private String service_version_wsh;

    /** 时区，固定 Asia/Shanghai */
    private String timezone_wsh;

    /** 槽位分钟粒度 */
    private Integer slot_minutes_wsh;

    /** 营业窗口来源：business_hours | legacy_unrestricted */
    private String schedule_source_wsh;

    /** 查询生成时间 */
    private LocalDateTime generated_at_wsh;

    /** 按天拆分的可预约性 */
    private List<DayAvailabilityVO> days_wsh;
}