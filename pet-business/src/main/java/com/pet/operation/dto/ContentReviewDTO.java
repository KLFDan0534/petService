package com.pet.operation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ContentReviewDTO {
    @Schema(description = "审核记录ID")
    private Long id_wsh;
    @Schema(description = "目标类型")
    private String target_type_wsh;
    @Schema(description = "目标ID")
    private Long target_id_wsh;
    @Schema(description = "举报人ID")
    private Long reporter_id_wsh;
    @Schema(description = "举报原因")
    private String reason_wsh;
    @Schema(description = "审核状态")
    private String status_wsh;
    @Schema(description = "审核人ID")
    private Long reviewer_id_wsh;
    @Schema(description = "审核备注")
    private String review_remark_wsh;
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
}