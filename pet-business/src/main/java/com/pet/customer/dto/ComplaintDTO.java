package com.pet.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ComplaintDTO {
    @Schema(description = "投诉ID")
    private Long id_wsh;
    @Schema(description = "关联订单ID")
    private Long order_id_wsh;
    @Schema(description = "所属商家ID")
    private Long merchant_id_wsh;
    @Schema(description = "投诉人ID")
    private Long owner_id_wsh;
    @Schema(description = "投诉目标ID")
    private Long target_id_wsh;
    @Schema(description = "投诉目标类型")
    private String target_type_wsh;
    @Schema(description = "投诉人姓名")
    private String owner_name_wsh;
    @Schema(description = "投诉标题")
    private String title_wsh;
    @Schema(description = "投诉内容")
    private String content_wsh;
    @Schema(description = "图片列表")
    private String images_wsh;
    @Schema(description = "投诉状态")
    private String status_wsh;
    @Schema(description = "处理结果")
    private String result_wsh;
    @Schema(description = "证据摘要")
    private ComplaintEvidenceSummaryDTO evidence_summary_wsh;
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
}
