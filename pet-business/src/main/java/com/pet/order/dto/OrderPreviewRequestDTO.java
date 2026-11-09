package com.pet.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 【价格预览请求】
 *
 * 业务作用：
 * 与下单请求同构，但不要求紧急联系人等「只有落单才需要」的字段，供前端在选择时间/宠物/
 * 优惠券的过程中实时询价。
 *
 * 设计约束：
 * 字段名必须与 {@link OrderCreateRequestDTO} 保持一致，服务端用 BeanUtils 直接拷贝，
 * 从而保证「预览用的参数」与「下单用的参数」是同一批字段，不会出现预览走 A 字段、
 * 下单走 B 字段的错位。
 */
@Getter
@Setter
public class OrderPreviewRequestDTO {

    @Schema(description = "宠物ID（用于冲突校验）")
    @NotNull(message = "宠物ID不能为空")
    private Long pet_id_wsh;

    @Schema(description = "商家ID（兼容字段；商家由服务端根据服务派生）")
    private Long merchant_id_wsh;

    @Schema(description = "服务ID")
    @NotNull(message = "服务ID不能为空")
    private Long service_id_wsh;

    @Schema(description = "服务版本（可选；有值时校验版本是否已变化）")
    private String service_version_wsh;

    @Schema(description = "客户端期望的单价（可选；有值时校验价格是否已变更）")
    private BigDecimal expected_unit_price_wsh;

    @Schema(description = "用户优惠券ID（可选；有值时预览该券的抵扣效果）")
    private Long user_coupon_id_wsh;

    @Schema(description = "开始日期（day 模式必填）")
    private LocalDate start_date_wsh;

    @Schema(description = "结束日期（day 模式必填；最后一个服务日，含当日）")
    private LocalDate end_date_wsh;

    @Schema(description = "服务开始时间（session/hour 模式必填）")
    private LocalDateTime start_time_wsh;

    @Schema(description = "客户端提交的服务结束时间（可选；仅做一致性校验）")
    private LocalDateTime end_time_wsh;

    @Schema(description = "计费单位（可选；规范化后必须与服务单位一致）")
    @Size(max = 20, message = "计费单位不能超过20个字符")
    private String billing_unit_wsh;

    @Schema(description = "计费数量（可选；day=天数、session=1、hour=小时数）")
    private Integer quantity_wsh;

    @Schema(description = "服务开始时间/送达时间（day 与 boarding 均使用）")
    private LocalDateTime delivery_time_wsh;

    @Schema(description = "服务结束时间/接回时间（day 与 boarding 均使用）")
    private LocalDateTime pickup_time_wsh;
}
