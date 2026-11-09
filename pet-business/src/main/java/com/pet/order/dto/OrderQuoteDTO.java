package com.pet.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 【价格预览响应】
 *
 * 业务作用：
 * 一次性返回「服务时间语义 + 金额构成」，前端直接渲染，不再自行做任何乘除。
 *
 * 字段口径：
 * - total_amount_wsh  基础价（未减任何优惠）
 * - long_stay_discount_wsh  长住折扣（day 按天数、boarding 按完整 24 小时周期数）
 * - coupon_discount_wsh     优惠券抵扣
 * - membership_discount_wsh 会员折扣
 * - discount_wsh            三者之和
 * - final_amount_wsh        实付 = 基础价 - discount
 *
 * 派生字段（total_minutes_wsh / billable_hours_wsh / days_wsh）仅用于展示与解释，
 * 不参与金额计算；金额只能由服务端返回，前端不得反推。
 */
@Getter
@Setter
public class OrderQuoteDTO {

    @Schema(description = "计费单位（服务端权威值）")
    private String billing_unit_wsh;

    @Schema(description = "计费数量：day=天数、session=1、hour=小时数、boarding=1")
    private Integer quantity_wsh;

    @Schema(description = "单单位时长（分钟）：day/boarding=1440，session/hour=服务配置时长")
    private Integer duration_minutes_wsh;

    @Schema(description = "兼容展示天数：day=天数、boarding=计费小时折算天数、session/hour=1")
    private Integer days_wsh;

    @Schema(description = "单价（每单位；boarding 为每 24 小时）")
    private BigDecimal unit_price_wsh;

    @Schema(description = "基础价（未减优惠）")
    private BigDecimal total_amount_wsh;

    @Schema(description = "长住折扣")
    private BigDecimal long_stay_discount_wsh;

    @Schema(description = "优惠券抵扣")
    private BigDecimal coupon_discount_wsh;

    @Schema(description = "会员折扣")
    private BigDecimal membership_discount_wsh;

    @Schema(description = "平台补贴（券补贴 + 会员折扣，商家结算不受影响的部分）")
    private BigDecimal platform_subsidy_wsh;

    @Schema(description = "优惠合计")
    private BigDecimal discount_wsh;

    @Schema(description = "实付金额")
    private BigDecimal final_amount_wsh;

    @Schema(description = "服务开始日期（含）")
    private LocalDate start_date_wsh;

    @Schema(description = "最后服务日（含）")
    private LocalDate end_date_wsh;

    @Schema(description = "服务开始时刻")
    private LocalDateTime delivery_time_wsh;

    @Schema(description = "服务结束时刻")
    private LocalDateTime pickup_time_wsh;

    @Schema(description = "命中的优惠券ID")
    private Long coupon_id_wsh;

    @Schema(description = "命中的会员权益ID")
    private Long membership_id_wsh;

    @Schema(description = "寄养实际照护总分钟数（仅 boarding 返回）")
    private Long total_minutes_wsh;

    @Schema(description = "寄养计费小时数（仅 boarding 返回；零头 ≤30 分钟丢弃、>30 进一小时）")
    private Long billable_hours_wsh;

    // ==================== 按夜计（night）报价字段 ====================

    @Schema(description = "预订夜数 N（仅 night 返回；不因提前加收而改变）")
    private Integer booked_nights_wsh;

    @Schema(description = "提前送达加收的整夜数（提前超过 3 小时为 1，否则 0）")
    private Integer early_extra_nights_wsh;

    @Schema(description = "提前送达按小时计费的小时数（0..3，超过 3 小时时为 0）")
    private Integer early_extra_hours_wsh;

    @Schema(description = "提前送达费（分项显示用；与夜费合计等于初始原价）")
    private BigDecimal early_amount_wsh;

    @Schema(description = "标准离店锚点：入住日 D + N 日 14:00")
    private LocalDateTime standard_checkout_at_wsh;

    @Schema(description = "免费接走截止：标准离店锚点 + 30 分钟")
    private LocalDateTime free_checkout_until_wsh;

    @Schema(description = "预计晚接小时数（仅展示，不参与本次支付）")
    private Integer estimated_overtime_hours_wsh;

    @Schema(description = "预计晚接补费金额（仅展示，实际接走时结算）")
    private BigDecimal estimated_overtime_amount_wsh;
}
