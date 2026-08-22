package com.pet.order.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 批量创建订单请求（多宠物连续下单）。
 *
 * <p>共享字段与单笔 {@link OrderCreateRequestDTO} 语义一致；每只宠物通过
 * {@code items} 独立提交（各自日期区间）。服务端在一个事务内为每只宠物创建
 * 一个独立订单，任一失败整批回滚；一张优惠券只应用于批次中金额最大的订单。</p>
 */
@Getter
@Setter
public class OrderBatchCreateRequestDTO {

    /** 批量下单最少宠物数 */
    public static final int MIN_ITEMS = 2;

    /** 批量下单最多宠物数 */
    public static final int MAX_ITEMS = 10;

    @Schema(description = "托管人ID")
    @NotNull(message = "keeperId cannot be empty")
    @JsonAlias("keeperId")
    private Long keeper_id_wsh;

    @Schema(description = "商家ID（兼容旧客户端字段；商家由服务端根据服务派生，不一致时拒绝）")
    @JsonAlias("merchantId")
    private Long merchant_id_wsh;

    @Schema(description = "服务ID（产品预订必填）")
    @NotNull(message = "serviceId cannot be empty")
    @JsonAlias("serviceId")
    private Long service_id_wsh;

    @Schema(description = "服务版本（来自服务详情/可预约性响应）")
    @JsonAlias("serviceVersion")
    private String service_version_wsh;

    @Schema(description = "客户端期望的单价（可选；有值时服务端必须与服务单价一致）")
    @JsonAlias("expectedUnitPrice")
    private BigDecimal expected_unit_price_wsh;

    @Schema(description = "计费单位（可选；规范化后必须与服务单位一致）")
    @Size(max = 20, message = "计费单位不能超过20个字符")
    @JsonAlias("billingUnit")
    private String billing_unit_wsh;

    @Schema(description = "用户优惠券ID（可选；只应用于批次中金额最大的订单）")
    @JsonAlias("userCouponId")
    private Long user_coupon_id_wsh;

    @Schema(description = "配送地址")
    @Size(max = 500, message = "deliveryAddress cannot exceed 500 characters")
    @JsonAlias("deliveryAddress")
    private String delivery_address_wsh;

    @Schema(description = "配送位置来源")
    @JsonAlias("deliveryLocationSource")
    private String delivery_location_source_wsh;

    @Schema(description = "接宠地址")
    @Size(max = 500, message = "pickupAddress cannot exceed 500 characters")
    @JsonAlias("pickupAddress")
    private String pickup_address_wsh;

    @Schema(description = "接宠位置来源")
    @JsonAlias("pickupLocationSource")
    private String pickup_location_source_wsh;

    @Schema(description = "紧急联系人姓名")
    @NotBlank(message = "emergencyContactName cannot be empty")
    @Size(max = 50, message = "emergencyContactName cannot exceed 50 characters")
    @JsonAlias("emergencyContactName")
    private String emergency_contact_name_wsh;

    @Schema(description = "紧急联系人电话")
    @NotBlank(message = "emergencyContactPhone cannot be empty")
    @Size(max = 20, message = "emergencyContactPhone cannot exceed 20 characters")
    @JsonAlias("emergencyContactPhone")
    private String emergency_contact_phone_wsh;

    @Schema(description = "备注")
    @Size(max = 200, message = "remark cannot exceed 200 characters")
    @JsonAlias("remark")
    private String remark_wsh;

    @Schema(description = "宠物下单项（2..10 项）")
    @NotEmpty(message = "items cannot be empty")
    @Size(min = MIN_ITEMS, max = MAX_ITEMS, message = "批量下单需要 2..10 只宠物")
    @Valid
    private List<OrderBatchItemDTO> items;
}
