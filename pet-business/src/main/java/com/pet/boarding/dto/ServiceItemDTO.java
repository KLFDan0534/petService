package com.pet.boarding.dto;

import lombok.Data;
import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 服务项目数据传输对象
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Data
public class ServiceItemDTO {
    @Schema(description = "服务ID")
    private Long id_wsh;
    @Schema(description = "商家ID")
    private Long merchant_id_wsh;
    @Schema(description = "服务名称")
    private String name_wsh;
    @Schema(description = "服务类型")
    private String type_wsh;
    @Schema(description = "分类ID")
    private Long category_id_wsh;
    @Schema(description = "分类名称")
    private String category_name_wsh;
    @Schema(description = "服务描述")
    private String description_wsh;
    @Schema(description = "价格")
    private BigDecimal price_wsh;
    @Schema(description = "单位")
    private String unit_wsh;
    @Schema(description = "图片")
    private String images_wsh;
    @Schema(description = "状态")
    private Integer status_wsh;
}