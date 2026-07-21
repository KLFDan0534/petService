package com.pet.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TicketMessageDTO {
    @Schema(description = "消息ID")
    private Long id_wsh;
    @Schema(description = "工单ID")
    private Long ticket_id_wsh;
    @Schema(description = "用户ID")
    private Long user_id_wsh;
    @Schema(description = "消息内容")
    private String content_wsh;
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
}