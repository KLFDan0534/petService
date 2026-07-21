package com.pet.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionDTO {
    @Schema(description = "交易记录ID")
    private Long id_wsh;
    @Schema(description = "钱包ID")
    private Long wallet_id_wsh;
    @Schema(description = "用户ID")
    private Long user_id_wsh;
    @Schema(description = "交易类型")
    private String type_wsh;
    @Schema(description = "交易金额")
    private BigDecimal amount_wsh;
    @Schema(description = "交易前余额")
    private BigDecimal balance_before_wsh;
    @Schema(description = "交易后余额")
    private BigDecimal balance_after_wsh;
    @Schema(description = "交易前冻结金额")
    private BigDecimal frozen_before_wsh;
    @Schema(description = "交易后冻结金额")
    private BigDecimal frozen_after_wsh;
    @Schema(description = "交易方向")
    private String direction_wsh;
    @Schema(description = "交易状态")
    private String status_wsh;
    @Schema(description = "业务类型")
    private String business_type_wsh;
    @Schema(description = "业务ID")
    private String business_id_wsh;
    @Schema(description = "请求ID（幂等）")
    private String request_id_wsh;
    @Schema(description = "关联订单ID")
    private Long order_id_wsh;
    @Schema(description = "交易描述")
    private String description_wsh;
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
}
