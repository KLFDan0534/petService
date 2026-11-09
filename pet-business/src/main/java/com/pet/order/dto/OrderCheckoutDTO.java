package com.pet.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 【确认接走结果】
 *
 * 业务作用：
 * 返回本次接走确认冻结的计费事实，供 keeper 端提示“已完成”或“待主人补费”。
 * 重复提交同一接走码时返回完全相同的结果（不重新计时、不生成第二笔补费单）。
 *
 * 字段口径：
 * - completed_wsh=true 表示订单已完成并释放容量；
 * - false 且 overtime_pay_no_wsh 非空表示接走已确认、等待主人支付补费，订单仍占容量。
 */
@Getter
@Setter
public class OrderCheckoutDTO {

    @Schema(description = "首次合法接走确认的服务端时间（冻结值）")
    private LocalDateTime checkout_confirmed_at_wsh;

    @Schema(description = "实际晚接小时数 H")
    private Integer overtime_hours_wsh;

    @Schema(description = "晚接补费金额")
    private BigDecimal overtime_amount_wsh;

    @Schema(description = "晚接补费支付单号；无补费时为 null")
    private String overtime_pay_no_wsh;

    @Schema(description = "订单是否已完成（false 表示待主人补费）")
    private Boolean completed_wsh;
}
