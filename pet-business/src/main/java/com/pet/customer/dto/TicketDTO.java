package com.pet.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TicketDTO {
    @Schema(description = "工单ID")
    private Long id_wsh;
    @Schema(description = "用户ID")
    private Long user_id_wsh;
    @Schema(description = "所属商家ID")
    private Long merchant_id_wsh;
    @Schema(description = "关联订单ID")
    private Long order_id_wsh;
    @Schema(description = "工单标题")
    private String title_wsh;
    @Schema(description = "工单内容")
    private String content_wsh;
    @Schema(description = "工单分类")
    private String category_wsh;
    @Schema(description = "工单优先级")
    private String priority_wsh;
    @Schema(description = "工单状态")
    private String status_wsh;
    @Schema(description = "处理结果")
    private String result_wsh;
    @Schema(description = "处理人ID")
    private Long assignee_id_wsh;
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
    @Schema(description = "更新时间")
    private LocalDateTime updated_at_wsh;
}
