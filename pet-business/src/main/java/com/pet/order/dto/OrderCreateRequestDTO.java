package com.pet.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class OrderCreateRequestDTO {
    @Schema(description = "宠物ID")
    @NotNull(message = "petId cannot be empty")
    private Long pet_id_wsh;

    @Schema(description = "托管人ID")
    @NotNull(message = "keeperId cannot be empty")
    private Long keeper_id_wsh;

    @Schema(description = "商家ID（兼容窗口内的旧客户端字段；商家由服务端根据服务派生，本字段不参与商家选择，不一致时拒绝）")
    private Long merchant_id_wsh;

    @Schema(description = "服务ID（产品预订必填；服务端根据服务派生商家与价格）")
    @NotNull(message = "serviceId cannot be empty")
    private Long service_id_wsh;

    @Schema(description = "服务版本（来自服务详情/可预约性响应；为空时跳过版本校验，兼容旧客户端）")
    private String service_version_wsh;

    @Schema(description = "客户端期望的单价（可选；有值时服务端必须与服务单价一致，否则 PRICE_CHANGED）")
    private BigDecimal expected_unit_price_wsh;

    @Schema(description = "用户优惠券ID")
    private Long user_coupon_id_wsh;

    @Schema(description = "开始日期（day 模式必填；session/hour 可省略，由服务端根据开始时间推导）")
    private LocalDate start_date_wsh;

    @Schema(description = "结束日期（day 模式必填；session/hour 由服务端按开始时间+时长计算，客户端可提交做一致性校验）")
    private LocalDate end_date_wsh;

    @Schema(description = "服务开始时间（session/hour 模式必填，来自可用性槽位；服务端据此计算结束时间）")
    private LocalDateTime start_time_wsh;

    @Schema(description = "客户端提交的服务结束时间（可选；仅做一致性校验，最终以服务端计算为准）")
    private LocalDateTime end_time_wsh;

    @Schema(description = "计费单位（可选；规范化后必须与服务单位一致，不一致返回 UNIT_MISMATCH）")
    @Size(max = 20, message = "计费单位不能超过20个字符")
    private String billing_unit_wsh;

    @Schema(description = "计费数量（可选；day=服务天数、session=1、hour=连续小时数，服务端重算校验）")
    private Integer quantity_wsh;

    @Schema(description = "配送地址")
    @Size(max = 500, message = "deliveryAddress cannot exceed 500 characters")
    private String delivery_address_wsh;

    @Schema(description = "配送纬度")
    private BigDecimal delivery_latitude_wsh;

    @Schema(description = "配送经度")
    private BigDecimal delivery_longitude_wsh;

    @Schema(description = "配送位置来源")
    private String delivery_location_source_wsh;

    @Schema(description = "配送时间")
    private LocalDateTime delivery_time_wsh;

    @Schema(description = "接收人可用开始时间")
    private LocalDateTime receiver_available_start_wsh;

    @Schema(description = "接收人可用结束时间")
    private LocalDateTime receiver_available_end_wsh;

    @Schema(description = "紧急联系人姓名")
    @NotBlank(message = "emergencyContactName cannot be empty")
    @Size(max = 50, message = "emergencyContactName cannot exceed 50 characters")
    private String emergency_contact_name_wsh;

    @Schema(description = "紧急联系人电话")
    @NotBlank(message = "emergencyContactPhone cannot be empty")
    @Size(max = 20, message = "emergencyContactPhone cannot exceed 20 characters")
    private String emergency_contact_phone_wsh;

    @Schema(description = "接宠时间")
    private LocalDateTime pickup_time_wsh;

    @Schema(description = "接宠地址")
    @Size(max = 500, message = "pickupAddress cannot exceed 500 characters")
    private String pickup_address_wsh;

    @Schema(description = "接宠纬度")
    private BigDecimal pickup_latitude_wsh;

    @Schema(description = "接宠经度")
    private BigDecimal pickup_longitude_wsh;

    @Schema(description = "接宠位置来源")
    private String pickup_location_source_wsh;

    @Schema(description = "备注")
    @Size(max = 200, message = "remark cannot exceed 200 characters")
    private String remark_wsh;
}
