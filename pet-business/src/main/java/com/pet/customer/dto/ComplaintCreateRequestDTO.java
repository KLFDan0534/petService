package com.pet.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ComplaintCreateRequestDTO {
    @Schema(description = "投诉标题")
    @NotBlank(message = "投诉标题不能为空")
    private String title_wsh;

    @Schema(description = "投诉内容")
    @NotBlank(message = "投诉内容不能为空")
    private String content_wsh;

    @Schema(description = "关联订单ID")
    private Long order_id_wsh;

    @Schema(description = "所属商家ID")
    private Long merchant_id_wsh;

    @Schema(description = "投诉目标ID")
    private Long target_id_wsh;

    @Schema(description = "投诉目标类型")
    private String target_type_wsh;

    @Schema(description = "图片列表")
    private String images_wsh;
}
