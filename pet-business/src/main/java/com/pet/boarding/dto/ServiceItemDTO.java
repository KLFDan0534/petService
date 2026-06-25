package com.pet.boarding.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 服务项目数据传输对象
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Data
public class ServiceItemDTO {
    private Long id_wsh;
    private Long merchant_id_wsh;
    private String name_wsh;
    private String type_wsh;
    private String description_wsh;
    private BigDecimal price_wsh;
    private String unit_wsh;
    private String images_wsh;
    private Integer status_wsh;
}