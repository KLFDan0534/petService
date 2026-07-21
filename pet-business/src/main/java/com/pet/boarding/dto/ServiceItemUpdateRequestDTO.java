package com.pet.boarding.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
public class ServiceItemUpdateRequestDTO {

    @Schema(description = "服务名称")
        private String name_wsh;

    @Schema(description = "分类ID")
        private Long category_id_wsh;

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
