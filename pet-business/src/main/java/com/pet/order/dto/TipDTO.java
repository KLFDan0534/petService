package com.pet.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 小费数据传输对象
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Data
public class TipDTO {
    @Schema(description = "小费ID")
    private Long id_wsh;
    @Schema(description = "订单ID")
    private Long order_id_wsh;
    @Schema(description = "支付用户ID")
    private Long from_user_id_wsh;
    @Schema(description = "接收用户ID")
    private Long to_user_id_wsh;
    @Schema(description = "小费金额")
    private BigDecimal amount_wsh;
    @Schema(description = "小费留言")
    private String message_wsh;
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
}