package com.pet.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TicketCreateRequestDTO {
    @Schema(description = "所属商家ID")
    private Long merchant_id_wsh;

    @Schema(description = "关联订单ID")
    private Long order_id_wsh;

    @Schema(description = "工单标题")
    @NotBlank(message = "工单标题不能为空")
        private String title_wsh;

    @Schema(description = "工单内容")
    @NotBlank(message = "工单内容不能为空")
        private String content_wsh;

    @Schema(description = "工单分类")
        private String category_wsh;

    @Schema(description = "工单优先级")
        private String priority_wsh;
}
