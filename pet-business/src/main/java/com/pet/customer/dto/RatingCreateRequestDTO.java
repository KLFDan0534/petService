package com.pet.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RatingCreateRequestDTO {
    @Schema(description = "订单ID")
    private Long order_id_wsh;

    @Schema(description = "评价目标ID")
    private Long target_id_wsh;

    @Schema(description = "评价目标类型")
    private String target_type_wsh;

    @Schema(description = "评分")
    private Integer score_wsh;

    @Schema(description = "评价内容")
    private String content_wsh;

    @Schema(description = "图片列表")
    private String images_wsh;
}
