package com.pet.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 【客服工作台统计】当前客服的工作量概览。
 */
@Data
public class CsWorkbenchStatsDTO {
    @Schema(description = "待处理工单数")
    private long pending_tickets_wsh;
    @Schema(description = "我处理中的工单数")
    private long my_processing_tickets_wsh;
    @Schema(description = "待处理投诉数")
    private long pending_complaints_wsh;
    @Schema(description = "今日已解决工单数")
    private long resolved_tickets_today_wsh;
    @Schema(description = "今日已处理投诉数")
    private long resolved_complaints_today_wsh;
    @Schema(description = "服务商家数")
    private long merchant_count_wsh;
}
