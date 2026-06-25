package com.pet.boarding.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商家数据传输对象
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Data
public class MerchantDTO {
    private Long id_wsh;
    private Long user_id_wsh;
    private String name_wsh;
    private String phone_wsh;
    private String address_wsh;
    private BigDecimal latitude_wsh;
    private BigDecimal longitude_wsh;
    private String description_wsh;
    private String business_license_wsh;
    private BigDecimal rating_wsh;
    private Integer status_wsh;
    private LocalDateTime created_at_wsh;
}