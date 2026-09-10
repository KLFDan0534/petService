package com.pet.ai.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 智能下单 plan 阶段产出的下单快照，随方案 token 保存在内存缓存中，
 * 供 confirm 阶段直接落单使用（不落库）。
 */
@Data
public class AgentPlanSnapshot {

    /** 宠物 */
    private Long pet_id_wsh;
    private String pet_name_wsh;

    /** 看护人 */
    private Long keeper_id_wsh;
    private String keeper_name_wsh;

    /** 商家（地址用于接送地址展示） */
    private Long merchant_id_wsh;
    private String merchant_name_wsh;
    private String merchant_address_wsh;

    /** 服务 */
    private Long service_id_wsh;
    private String service_name_wsh;
    private String unit_wsh;

    /** 日期与天数 */
    private LocalDate start_date_wsh;
    private LocalDate end_date_wsh;
    private Integer days_wsh;

    /** 计价（confirm 时服务端二次校验价格未变） */
    private BigDecimal unit_price_wsh;
    private BigDecimal total_price_wsh;

    /** 看护人距离（公里，展示用） */
    private Double distance_wsh;

    /** 原始用户输入（写入订单备注） */
    private String user_input_wsh;
}