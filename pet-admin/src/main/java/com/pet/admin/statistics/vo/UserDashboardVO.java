package com.pet.admin.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class UserDashboardVO {
    @Schema(description = "宠物数量")
    private int pets_wsh;
    @Schema(description = "进行中订单数")
    private long active_orders_wsh;
    @Schema(description = "已完成订单数")
    private long completed_orders_wsh;
    @Schema(description = "总消费金额")
    private BigDecimal total_spent_wsh;
}
