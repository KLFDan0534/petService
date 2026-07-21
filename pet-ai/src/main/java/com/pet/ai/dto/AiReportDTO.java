package com.pet.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AiReportDTO {
    @Schema(description = "ID")
    private Long id_wsh;
    @Schema(description = "订单ID")
    private Long order_id_wsh;
    @Schema(description = "宠物ID")
    private Long pet_id_wsh;
    @Schema(description = "寄养人ID")
    private Long keeper_id_wsh;
    @Schema(description = "内容")
    private String content_wsh;
    @Schema(description = "类型")
    private String type_wsh;
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
}