package com.pet.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 【客服服务商家】客服当前服务的一家商家及其待办数量。
 */
@Data
public class CsMerchantDTO {
    @Schema(description = "商家ID")
    private Long merchant_id_wsh;
    @Schema(description = "商家名称")
    private String merchant_name_wsh;
    @Schema(description = "商家状态")
    private Integer merchant_status_wsh;
    @Schema(description = "该商家待处理工单数")
    private long pending_ticket_count_wsh;
    @Schema(description = "该商家待处理投诉数")
    private long pending_complaint_count_wsh;
}
