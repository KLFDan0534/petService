package com.pet.boarding.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 地址数据传输对象
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Data
public class AddressDTO {
    private Long id_wsh;
    private Long user_id_wsh;
    private String label_wsh;
    private String name_wsh;
    private String phone_wsh;
    private String address_wsh;
    private String detail_wsh;
    private BigDecimal latitude_wsh;
    private BigDecimal longitude_wsh;
    private Integer is_default_wsh;
}