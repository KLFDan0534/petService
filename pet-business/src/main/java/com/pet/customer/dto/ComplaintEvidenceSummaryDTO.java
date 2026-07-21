package com.pet.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ComplaintEvidenceSummaryDTO {
    @Schema(description = "订单ID")
    private Long order_id_wsh;
    @Schema(description = "订单号")
    private String order_no_wsh;
    @Schema(description = "订单状态")
    private String order_status_wsh;
    @Schema(description = "聊天消息数量")
    private Long chat_message_count_wsh;
    @Schema(description = "服务记录数量")
    private Long care_record_count_wsh;
    @Schema(description = "最新聊天时间")
    private LocalDateTime latest_chat_time_wsh;
    @Schema(description = "最新服务记录时间")
    private LocalDateTime latest_care_record_time_wsh;
}
