package com.pet.order.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单数据传输对象
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Data
public class OrderDTO {
    private Long id_wsh;
    private String order_no_wsh;
    private Long owner_id_wsh;
    private Long pet_id_wsh;
    private Long keeper_id_wsh;
    private Long merchant_id_wsh;
    private Long service_id_wsh;
    private LocalDate start_date_wsh;
    private LocalDate end_date_wsh;
    private Integer days_wsh;
    private BigDecimal price_per_day_wsh;
    private BigDecimal total_amount_wsh;
    private BigDecimal discount_wsh;
    private BigDecimal final_amount_wsh;
    private String status_wsh;
    private String remark_wsh;
    private LocalDateTime created_at_wsh;
}