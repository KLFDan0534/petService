package com.pet.boarding.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
public class KeeperCreateRequestDTO {
    @Schema(description = "看护者名称")
    private String name_wsh;

    @Schema(description = "手机号")
    private String phone_wsh;

    @Schema(description = "头像")
    private String avatar_wsh;

    @Schema(description = "从业经验年数")
    private Integer experience_years_wsh;

    @Schema(description = "每日价格")
    private BigDecimal price_per_day_wsh;

    @Schema(description = "最大接单数量")
    private Integer max_pets_wsh;

    @Schema(description = "简介")
    private String bio_wsh;

    @Schema(description = "商家ID")
    private Long merchant_id_wsh;

    @Schema(description = "资质图片")
    private String qualification_image_wsh;
}
