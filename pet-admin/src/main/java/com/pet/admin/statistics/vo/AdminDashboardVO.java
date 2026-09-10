package com.pet.admin.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class AdminDashboardVO {
    @Schema(description = "总用户数")
    private int total_users_wsh;
    @Schema(description = "总宠物数")
    private int total_pets_wsh;
    @Schema(description = "总商户数")
    private int total_merchants_wsh;
    @Schema(description = "总看护者数")
    private int total_keepers_wsh;
    @Schema(description = "总订单数")
    private int total_orders_wsh;
    @Schema(description = "总营收")
    private BigDecimal total_revenue_wsh;
    @Schema(description = "待处理订单数")
    private long pending_orders_wsh;
    @Schema(description = "已完成订单数")
    private long completed_orders_wsh;
}
