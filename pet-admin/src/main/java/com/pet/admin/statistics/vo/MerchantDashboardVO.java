package com.pet.admin.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class MerchantDashboardVO {
    @Schema(description = "总订单数")
    private int total_orders_wsh;
    @Schema(description = "总营收")
    private BigDecimal total_revenue_wsh;
    @Schema(description = "待处理订单数")
    private long pending_orders_wsh;
    @Schema(description = "进行中订单数")
    private long active_orders_wsh;
    @Schema(description = "已完成订单数")
    private long completed_orders_wsh;
    @Schema(description = "宠物数量")
    private int pets_wsh;
}
