package com.pet.boarding.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
public class ServiceItemCreateRequestDTO {

    @Schema(description = "商家ID")
    @NotNull(message = "鍟嗗ID涓嶈兘涓虹┖")
        private Long merchant_id_wsh;

    @Schema(description = "服务名称")
    @NotBlank(message = "鏈嶅姟鍚嶇О涓嶈兘涓虹┖")
        private String name_wsh;

    @Schema(description = "服务分类ID")
    @NotNull(message = "服务分类不能为空")
        private Long category_id_wsh;

    @Schema(description = "服务描述")
        private String description_wsh;

    @Schema(description = "价格")
    @NotNull(message = "浠锋牸涓嶈兘涓虹┖")
        private BigDecimal price_wsh;

    @Schema(description = "单位")
        private String unit_wsh;

    @Schema(description = "图片")
        private String images_wsh;
}
