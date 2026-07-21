package com.pet.marketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CouponGrantRequestDTO {
    @Schema(description = "用户ID")
    private Long user_id_wsh;

    @Schema(description = "发放数量")
    private Integer quantity_wsh;

    @Schema(description = "注册开始时间")
    private LocalDateTime registered_from_wsh;

    @Schema(description = "注册结束时间")
    private LocalDateTime registered_to_wsh;

    @Schema(description = "最低累计消费金额")
    private BigDecimal min_total_spend_wsh;

    @Schema(description = "最低完成订单数")
    private Integer min_order_count_wsh;

    @Schema(description = "最低宠物数")
    private Integer min_pet_count_wsh;

    @Schema(description = "最近下单开始时间")
    private LocalDateTime last_order_from_wsh;

    @Schema(description = "最近下单结束时间")
    private LocalDateTime last_order_to_wsh;

    @Schema(description = "指定用户ID白名单")
    private List<Long> include_user_ids_wsh;

    @Schema(description = "排除用户ID黑名单")
    private List<Long> exclude_user_ids_wsh;
}
