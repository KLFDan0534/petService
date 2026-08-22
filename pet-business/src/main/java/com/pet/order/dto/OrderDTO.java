package com.pet.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单数据传输对象
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Data
public class OrderDTO {
    @Schema(description = "订单ID")
    private Long id_wsh;
    @Schema(description = "订单号")
    private String order_no_wsh;
    @Schema(description = "主人ID")
    private Long owner_id_wsh;
    @Schema(description = "宠物ID")
    private Long pet_id_wsh;
    @Schema(description = "托管人ID")
    private Long keeper_id_wsh;
    @Schema(description = "商家ID")
    private Long merchant_id_wsh;
    @Schema(description = "服务ID")
    private Long service_id_wsh;
    @Schema(description = "服务名称")
    private String service_name_wsh;
    @Schema(description = "服务描述")
    private String service_description_wsh;
    @Schema(description = "主人姓名")
    private String owner_name_wsh;
    @Schema(description = "宠物名称")
    private String pet_name_wsh;
    @Schema(description = "托管人姓名")
    private String keeper_name_wsh;
    @Schema(description = "托管人电话")
    private String keeper_phone_wsh;
    @Schema(description = "托管人头像")
    private String keeper_avatar_wsh;
    @Schema(description = "商家名称")
    private String merchant_name_wsh;
    @Schema(description = "商家电话")
    private String merchant_phone_wsh;
    @Schema(description = "商家地址")
    private String merchant_address_wsh;
    @Schema(description = "开始日期")
    private LocalDate start_date_wsh;
    @Schema(description = "结束日期")
    private LocalDate end_date_wsh;
    @Schema(description = "天数（兼容投影）")
    private Integer days_wsh;
    @Schema(description = "每日价格（兼容投影）")
    private BigDecimal price_per_day_wsh;
    @Schema(description = "计费单位（day/session/hour）")
    private String billing_unit_wsh;
    @Schema(description = "计费数量")
    private Integer quantity_wsh;
    @Schema(description = "单价（下单时服务单价快照）")
    private BigDecimal unit_price_wsh;
    @Schema(description = "单次服务时长（分钟）")
    private Integer duration_minutes_wsh;
    @Schema(description = "总金额")
    private BigDecimal total_amount_wsh;
    @Schema(description = "折扣金额")
    private BigDecimal discount_wsh;
    @Schema(description = "优惠券ID")
    private Long coupon_id_wsh;
    @Schema(description = "优惠券模板ID")
    private Long coupon_template_id_wsh;
    @Schema(description = "优惠券折扣金额")
    private BigDecimal coupon_discount_wsh;
    @Schema(description = "会员ID")
    private Long membership_id_wsh;
    @Schema(description = "会员套餐ID")
    private Long membership_plan_id_wsh;
    @Schema(description = "会员折扣金额")
    private BigDecimal membership_discount_wsh;
    @Schema(description = "会员权益快照")
    private String membership_snapshot_wsh;
    @Schema(description = "平台补贴")
    private BigDecimal platform_subsidy_wsh;
    @Schema(description = "结算金额")
    private BigDecimal settlement_amount_wsh;
    @Schema(description = "促销快照")
    private String promotion_snapshot_wsh;
    @Schema(description = "最终金额")
    private BigDecimal final_amount_wsh;
    @Schema(description = "订单状态")
    private String status_wsh;
    @Schema(description = "备注")
    private String remark_wsh;
    @Schema(description = "订单快照")
    private OrderSnapshotDTO snapshot_wsh;
    @Schema(description = "交接码")
    private String handover_code_wsh;
    @Schema(description = "配送地址")
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
    private String emergency_contact_name_wsh;
    @Schema(description = "紧急联系人电话")
    private String emergency_contact_phone_wsh;
    @Schema(description = "接宠地址")
    private String pickup_address_wsh;
    @Schema(description = "接宠纬度")
    private BigDecimal pickup_latitude_wsh;
    @Schema(description = "接宠经度")
    private BigDecimal pickup_longitude_wsh;
    @Schema(description = "接宠位置来源")
    private String pickup_location_source_wsh;
    @Schema(description = "接宠时间")
    private LocalDateTime pickup_time_wsh;
    @Schema(description = "送达时间")
    private LocalDateTime delivered_at_wsh;
    @Schema(description = "送达地址")
    private String delivered_address_wsh;
    @Schema(description = "送达纬度")
    private BigDecimal delivered_latitude_wsh;
    @Schema(description = "送达经度")
    private BigDecimal delivered_longitude_wsh;
    @Schema(description = "送达精度")
    private BigDecimal delivered_accuracy_wsh;
    @Schema(description = "接收时间")
    private LocalDateTime received_at_wsh;
    @Schema(description = "接收地址")
    private String received_address_wsh;
    @Schema(description = "接收纬度")
    private BigDecimal received_latitude_wsh;
    @Schema(description = "接收经度")
    private BigDecimal received_longitude_wsh;
    @Schema(description = "接收精度")
    private BigDecimal received_accuracy_wsh;
    @Schema(description = "接收距离（米）")
    private BigDecimal received_distance_m_wsh;
    @Schema(description = "开始时间")
    private LocalDateTime started_at_wsh;
    @Schema(description = "开始照片")
    private String start_photo_wsh;
    @Schema(description = "完成时间")
    private LocalDateTime completed_at_wsh;
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
    @Schema(description = "是否已有反馈（评价）")
    private Boolean has_feedback_wsh;

    @JsonProperty("id")
    public Long getId() { return id_wsh; }

    @JsonProperty("orderNo")
    public String getOrderNo() { return order_no_wsh; }

    @JsonProperty("status")
    public String getStatus() { return status_wsh; }

    @JsonProperty("amount")
    public BigDecimal getAmount() { return final_amount_wsh; }

    @JsonProperty("startDate")
    public LocalDate getStartDate() { return start_date_wsh; }

    @JsonProperty("endDate")
    public LocalDate getEndDate() { return end_date_wsh; }

    @JsonProperty("handoverCode")
    public String getHandoverCode() { return handover_code_wsh; }

    @JsonProperty("emergencyContactName")
    public String getEmergencyContactName() { return emergency_contact_name_wsh; }

    @JsonProperty("emergencyContactPhone")
    public String getEmergencyContactPhone() { return emergency_contact_phone_wsh; }
}
