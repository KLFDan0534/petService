package com.pet.boarding.vo;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

/**
 * 看护者视图对象
 * 包含看护者基本信息及关联商户的距离等信息
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Getter
@Setter
public class KeeperVO {
    private Long id_wsh;
    private Long merchant_id_wsh;
    private String merchant_name_wsh;
    private Long user_id_wsh;
    private String name_wsh;
    private String phone_wsh;
    private String avatar_wsh;
    private Integer experience_years_wsh;
    private BigDecimal rating_wsh;
    private BigDecimal completion_rate_wsh;
    private BigDecimal complaint_rate_wsh;
    private BigDecimal price_per_day_wsh;
    private Integer max_pets_wsh;
    private Integer current_pets_wsh;
    private Integer status_wsh;
    private Double distance_wsh;
    private BigDecimal merchant_latitude_wsh;
    private BigDecimal merchant_longitude_wsh;
}
