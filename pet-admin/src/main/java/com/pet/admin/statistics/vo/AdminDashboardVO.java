package com.pet.admin.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class AdminDashboardVO {
    @Schema(description = "总用户数")
    private int totalUsers;
    @Schema(description = "总宠物数")
    private int totalPets;
    @Schema(description = "总商户数")
    private int totalMerchants;
    @Schema(description = "总看护者数")
    private int totalKeepers;
    @Schema(description = "总订单数")
    private int totalOrders;
    @Schema(description = "总营收")
    private BigDecimal totalRevenue;
    @Schema(description = "待处理订单数")
    private long pendingOrders;
    @Schema(description = "已完成订单数")
    private long completedOrders;
}
