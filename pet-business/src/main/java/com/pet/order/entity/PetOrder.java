package com.pet.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Core domain entity representing a pet boarding order.
 * <p>
 * This is the central entity of the pet boarding platform. An order tracks
 * the full lifecycle of a pet boarding service from creation through completion.
 * <p>
 * <b>State machine:</b>
 * <pre>
 * PENDING → PAID → CONFIRMED → DELIVERED → RECEIVED → IN_PROGRESS → COMPLETED
 *   │         │
 *   └→ CANCELLED  └→ CANCELLED (reject with refund)
 * </pre>
 * <ul>
 *   <li><b>PENDING:</b> Order created, awaiting payment (15-min timeout)</li>
 *   <li><b>PAID:</b> Payment successful, awaiting keeper acceptance</li>
 *   <li><b>CONFIRMED:</b> Keeper accepted, awaiting pet delivery</li>
 *   <li><b>DELIVERED:</b> Owner delivered the pet to the keeper/merchant</li>
 *   <li><b>RECEIVED:</b> Keeper/merchant received the pet (handover code verified)</li>
 *   <li><b>IN_PROGRESS:</b> Service started with start photo documentation</li>
 *   <li><b>COMPLETED:</b> Service ended, triggers settlement and AI report generation</li>
 *   <li><b>CANCELLED:</b> Order cancelled before payment or rejected after payment</li>
 *   <li><b>REFUNDING:</b> Refund application in progress</li>
 *   <li><b>REFUNDED:</b> Refund completed, funds returned to owner</li>
 * </ul>
 * <p>
 * <b>Key business fields:</b>
 * <ul>
 *   <li>{@code handover_code} - 4-digit code used for pet delivery verification</li>
 *   <li>{@code final_amount} - the amount actually paid after all discounts</li>
 *   <li>{@code settlement_amount} - the amount settled to the merchant on completion</li>
 *   <li>{@code platform_subsidy} - coupon subsidy paid by the platform</li>
 * </ul>
 */
@Getter
@Setter
@TableName("pet_order_wsh")
@Schema(description = "宠物订单实体")
public class PetOrder {

    // TODO 后期需要修改字段序列化,去掉Json注解,并且给前端使用当前数据的字段增加 _wsh后最

    @JsonProperty("id_wsh")
    @TableId(value = "id_wsh", type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id_wsh;

    @JsonProperty("order_no_wsh")
    @TableField(value = "order_no_wsh")
    @Schema(description = "订单号")
    private String order_no_wsh;

    @JsonProperty("owner_id_wsh")
    @TableField(value = "owner_id_wsh")
    @Schema(description = "宠物主用户ID")
    private Long owner_id_wsh;

    @JsonProperty("pet_id_wsh")
    @TableField(value = "pet_id_wsh")
    @Schema(description = "宠物ID")
    private Long pet_id_wsh;

    @JsonProperty("keeper_id_wsh")
    @TableField(value = "keeper_id_wsh")
    @Schema(description = "看护者ID")
    private Long keeper_id_wsh;

    @JsonProperty("merchant_id_wsh")
    @TableField(value = "merchant_id_wsh")
    @Schema(description = "商家ID")
    private Long merchant_id_wsh;

    @JsonProperty("service_id_wsh")
    @TableField(value = "service_id_wsh")
    @Schema(description = "服务项目ID")
    private Long service_id_wsh;

    @JsonProperty("start_date_wsh")
    @JsonAlias({"startDate", "start_date"})
    @TableField(value = "start_date_wsh")
    @Schema(description = "开始日期")
    private LocalDate start_date_wsh;

    @JsonProperty("end_date_wsh")
    @JsonAlias({"endDate", "end_date"})
    @TableField(value = "end_date_wsh")
    @Schema(description = "结束日期")
    private LocalDate end_date_wsh;

    @JsonProperty("days_wsh")
    @TableField(value = "days_wsh")
    @Schema(description = "服务天数")
    private Integer days_wsh;

    @JsonProperty("price_per_day_wsh")
    @TableField(value = "price_per_day_wsh")
    @Schema(description = "每日价格")
    private BigDecimal price_per_day_wsh;

    @JsonProperty("total_amount_wsh")
    @TableField(value = "total_amount_wsh")
    @Schema(description = "总金额")
    private BigDecimal total_amount_wsh;

    @JsonProperty("discount_wsh")
    @TableField(value = "discount_wsh")
    @Schema(description = "折扣金额")
    private BigDecimal discount_wsh;

    @JsonProperty("coupon_id_wsh")
    @TableField(value = "coupon_id_wsh")
    @Schema(description = "优惠券ID")
    private Long coupon_id_wsh;

    @JsonProperty("coupon_template_id_wsh")
    @TableField(value = "coupon_template_id_wsh")
    @Schema(description = "优惠券模板ID")
    private Long coupon_template_id_wsh;

    @JsonProperty("coupon_discount_wsh")
    @TableField(value = "coupon_discount_wsh")
    @Schema(description = "优惠券抵扣金额")
    private BigDecimal coupon_discount_wsh;

    @JsonProperty("membership_id_wsh")
    @TableField(value = "membership_id_wsh")
    @Schema(description = "会员ID")
    private Long membership_id_wsh;

    @JsonProperty("membership_plan_id_wsh")
    @TableField(value = "membership_plan_id_wsh")
    @Schema(description = "会员套餐ID")
    private Long membership_plan_id_wsh;

    @JsonProperty("membership_discount_wsh")
    @TableField(value = "membership_discount_wsh")
    @Schema(description = "会员折扣金额")
    private BigDecimal membership_discount_wsh;

    @JsonProperty("membership_snapshot_wsh")
    @TableField(value = "membership_snapshot_wsh")
    @Schema(description = "会员权益快照")
    private String membership_snapshot_wsh;

    @JsonProperty("platform_subsidy_wsh")
    @TableField(value = "platform_subsidy_wsh")
    @Schema(description = "平台补贴金额")
    private BigDecimal platform_subsidy_wsh;

    @JsonProperty("settlement_amount_wsh")
    @TableField(value = "settlement_amount_wsh")
    @Schema(description = "结算金额")
    private BigDecimal settlement_amount_wsh;

    @JsonProperty("promotion_snapshot_wsh")
    @TableField(value = "promotion_snapshot_wsh")
    @Schema(description = "促销快照")
    private String promotion_snapshot_wsh;

    @JsonProperty("final_amount_wsh")
    @TableField(value = "final_amount_wsh")
    @Schema(description = "最终支付金额")
    private BigDecimal final_amount_wsh;

    @JsonProperty("status_wsh")
    @TableField(value = "status_wsh")
    @Schema(description = "状态")
    private String status_wsh;

    @JsonProperty("handover_code_wsh")
    @JsonAlias({"handoverCode", "handover_code", "receiveCode", "receive_code"})
    @TableField(value = "handover_code_wsh")
    @Schema(description = "交接码")
    private String handover_code_wsh;

    @JsonProperty("delivery_address_wsh")
    @JsonAlias({"deliveryAddress", "delivery_address", "dropoffAddress", "dropoff_address"})
    @TableField(value = "delivery_address_wsh")
    @Schema(description = "配送地址")
    private String delivery_address_wsh;

    @JsonProperty("delivery_latitude_wsh")
    @JsonAlias({"deliveryLatitude", "delivery_latitude"})
    @TableField(value = "delivery_latitude_wsh")
    @Schema(description = "配送纬度")
    private BigDecimal delivery_latitude_wsh;

    @JsonProperty("delivery_longitude_wsh")
    @JsonAlias({"deliveryLongitude", "delivery_longitude"})
    @TableField(value = "delivery_longitude_wsh")
    @Schema(description = "配送经度")
    private BigDecimal delivery_longitude_wsh;

    @JsonProperty("delivery_location_source_wsh")
    @JsonAlias({"deliveryLocationSource", "delivery_location_source"})
    @TableField(value = "delivery_location_source_wsh")
    @Schema(description = "配送位置来源")
    private String delivery_location_source_wsh;

    @JsonProperty("delivery_time_wsh")
    @JsonAlias({"deliveryTime", "delivery_time", "dropoffTime", "dropoff_time"})
    @TableField(value = "delivery_time_wsh")
    @Schema(description = "配送时间")
    private LocalDateTime delivery_time_wsh;

    @JsonProperty("receiver_available_start_wsh")
    @JsonAlias({"receiverAvailableStart", "receiver_available_start", "keeperAvailableStart", "keeper_available_start"})
    @TableField(value = "receiver_available_start_wsh")
    @Schema(description = "接宠人空闲开始时间")
    private LocalDateTime receiver_available_start_wsh;

    @JsonProperty("receiver_available_end_wsh")
    @JsonAlias({"receiverAvailableEnd", "receiver_available_end", "keeperAvailableEnd", "keeper_available_end"})
    @TableField(value = "receiver_available_end_wsh")
    @Schema(description = "接宠人空闲结束时间")
    private LocalDateTime receiver_available_end_wsh;

    @JsonProperty("emergency_contact_name_wsh")
    @JsonAlias({"emergencyContactName", "emergency_contact_name"})
    @TableField(value = "emergency_contact_name_wsh")
    @Schema(description = "紧急联系人姓名")
    private String emergency_contact_name_wsh;

    @JsonProperty("emergency_contact_phone_wsh")
    @JsonAlias({"emergencyContactPhone", "emergency_contact_phone"})
    @TableField(value = "emergency_contact_phone_wsh")
    @Schema(description = "紧急联系人电话")
    private String emergency_contact_phone_wsh;

    @JsonProperty("pickup_address_wsh")
    @JsonAlias({"pickupAddress", "pickup_address", "returnAddress", "return_address"})
    @TableField(value = "pickup_address_wsh")
    @Schema(description = "接宠地址")
    private String pickup_address_wsh;

    @JsonProperty("pickup_latitude_wsh")
    @JsonAlias({"pickupLatitude", "pickup_latitude"})
    @TableField(value = "pickup_latitude_wsh")
    @Schema(description = "接宠纬度")
    private BigDecimal pickup_latitude_wsh;

    @JsonProperty("pickup_longitude_wsh")
    @JsonAlias({"pickupLongitude", "pickup_longitude"})
    @TableField(value = "pickup_longitude_wsh")
    @Schema(description = "接宠经度")
    private BigDecimal pickup_longitude_wsh;

    @JsonProperty("pickup_location_source_wsh")
    @JsonAlias({"pickupLocationSource", "pickup_location_source"})
    @TableField(value = "pickup_location_source_wsh")
    @Schema(description = "接宠位置来源")
    private String pickup_location_source_wsh;

    @JsonProperty("pickup_time_wsh")
    @JsonAlias({"pickupTime", "pickup_time", "returnTime", "return_time"})
    @TableField(value = "pickup_time_wsh")
    @Schema(description = "接宠时间")
    private LocalDateTime pickup_time_wsh;

    @JsonProperty("delivered_at_wsh")
    @TableField(value = "delivered_at_wsh")
    @Schema(description = "送达时间")
    private LocalDateTime delivered_at_wsh;

    @JsonProperty("delivered_address_wsh")
    @JsonAlias({"deliveredAddress", "delivered_address"})
    @TableField(value = "delivered_address_wsh")
    @Schema(description = "送达地址")
    private String delivered_address_wsh;

    @JsonProperty("delivered_latitude_wsh")
    @JsonAlias({"deliveredLatitude", "delivered_latitude"})
    @TableField(value = "delivered_latitude_wsh")
    @Schema(description = "送达纬度")
    private BigDecimal delivered_latitude_wsh;

    @JsonProperty("delivered_longitude_wsh")
    @JsonAlias({"deliveredLongitude", "delivered_longitude"})
    @TableField(value = "delivered_longitude_wsh")
    @Schema(description = "送达经度")
    private BigDecimal delivered_longitude_wsh;

    @JsonProperty("delivered_accuracy_wsh")
    @JsonAlias({"deliveredAccuracy", "delivered_accuracy"})
    @TableField(value = "delivered_accuracy_wsh")
    @Schema(description = "送达定位精度")
    private BigDecimal delivered_accuracy_wsh;

    @JsonProperty("received_at_wsh")
    @TableField(value = "received_at_wsh")
    @Schema(description = "接宠时间")
    private LocalDateTime received_at_wsh;

    @JsonProperty("received_address_wsh")
    @JsonAlias({"receivedAddress", "received_address"})
    @TableField(value = "received_address_wsh")
    @Schema(description = "接收地址")
    private String received_address_wsh;

    @JsonProperty("received_latitude_wsh")
    @JsonAlias({"receivedLatitude", "received_latitude"})
    @TableField(value = "received_latitude_wsh")
    @Schema(description = "接收纬度")
    private BigDecimal received_latitude_wsh;

    @JsonProperty("received_longitude_wsh")
    @JsonAlias({"receivedLongitude", "received_longitude"})
    @TableField(value = "received_longitude_wsh")
    @Schema(description = "接收经度")
    private BigDecimal received_longitude_wsh;

    @JsonProperty("received_accuracy_wsh")
    @JsonAlias({"receivedAccuracy", "received_accuracy"})
    @TableField(value = "received_accuracy_wsh")
    @Schema(description = "接收定位精度")
    private BigDecimal received_accuracy_wsh;

    @JsonProperty("received_distance_m_wsh")
    @TableField(value = "received_distance_m_wsh")
    @Schema(description = "接收距离")
    private BigDecimal received_distance_m_wsh;

    @JsonProperty("started_at_wsh")
    @TableField(value = "started_at_wsh")
    @Schema(description = "服务开始时间")
    private LocalDateTime started_at_wsh;

    @JsonProperty("start_photo_wsh")
    @JsonAlias({"startPhoto", "start_photo", "startPhotoUrl", "start_photo_url_wsh"})
    @TableField(value = "start_photo_wsh")
    @Schema(description = "开始服务照片")
    private String start_photo_wsh;

    @JsonProperty("completed_at_wsh")
    @TableField(value = "completed_at_wsh")
    @Schema(description = "服务完成时间")
    private LocalDateTime completed_at_wsh;

    @JsonProperty("final_report_generated_wsh")
    @TableField(value = "final_report_generated_wsh")
    @Schema(description = "最终报告是否已生成")
    private Integer final_report_generated_wsh;    // 0:未生成 1:已生成

    @JsonProperty("remark_wsh")
    @TableField(value = "remark_wsh")
    @Schema(description = "备注")
    private String remark_wsh;

    @JsonProperty("service_name_wsh")
    @TableField(exist = false)
    @Schema(description = "服务名称")
    private String service_name_wsh;

    @JsonProperty("owner_name_wsh")
    @TableField(exist = false)
    @Schema(description = "宠物主姓名")
    private String owner_name_wsh;

    @JsonProperty("pet_name_wsh")
    @TableField(exist = false)
    @Schema(description = "宠物名称")
    private String pet_name_wsh;

    @JsonProperty("keeper_name_wsh")
    @TableField(exist = false)
    @Schema(description = "看护者姓名")
    private String keeper_name_wsh;

    @JsonProperty("keeper_phone_wsh")
    @TableField(exist = false)
    @Schema(description = "看护者电话")
    private String keeper_phone_wsh;

    @JsonProperty("keeper_avatar_wsh")
    @TableField(exist = false)
    @Schema(description = "看护者头像")
    private String keeper_avatar_wsh;

    @JsonProperty("merchant_name_wsh")
    @TableField(exist = false)
    @Schema(description = "商家名称")
    private String merchant_name_wsh;

    @JsonProperty("merchant_phone_wsh")
    @TableField(exist = false)
    @Schema(description = "商家电话")
    private String merchant_phone_wsh;

    @JsonProperty("merchant_address_wsh")
    @TableField(exist = false)
    @Schema(description = "商家地址")
    private String merchant_address_wsh;

    @JsonProperty("service_description_wsh")
    @TableField(exist = false)
    @Schema(description = "服务描述")
    private String service_description_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    @Schema(description = "逻辑删除标志")
    private Integer deleted_wsh;

    @JsonProperty("created_at_wsh")
    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;

    @JsonProperty("updated_at_wsh")
    @TableField(value = "updated_at_wsh", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updated_at_wsh;

    @JsonGetter("id")
    public Long getId() {
        return id_wsh;
    }

    @JsonGetter("orderNo")
    public String getOrderNo() {
        return order_no_wsh;
    }

    @JsonGetter("ownerId")
    public Long getOwnerId() {
        return owner_id_wsh;
    }

    @JsonGetter("petId")
    public Long getPetId() {
        return pet_id_wsh;
    }

    @JsonGetter("keeperId")
    public Long getKeeperId() {
        return keeper_id_wsh;
    }

    @JsonGetter("merchantId")
    public Long getMerchantId() {
        return merchant_id_wsh;
    }

    @JsonGetter("serviceId")
    public Long getServiceId() {
        return service_id_wsh;
    }

    @JsonGetter("startDate")
    public LocalDate getStartDate() {
        return start_date_wsh;
    }

    @JsonGetter("endDate")
    public LocalDate getEndDate() {
        return end_date_wsh;
    }

    @JsonGetter("status")
    public String getStatus() {
        return status_wsh;
    }

    @JsonGetter("handoverCode")
    public String getHandoverCode() {
        return handover_code_wsh;
    }

    @JsonGetter("deliveryAddress")
    public String getDeliveryAddress() {
        return delivery_address_wsh;
    }

    @JsonGetter("deliveryTime")
    public LocalDateTime getDeliveryTime() {
        return delivery_time_wsh;
    }

    @JsonGetter("receiverAvailableStart")
    public LocalDateTime getReceiverAvailableStart() {
        return receiver_available_start_wsh;
    }

    @JsonGetter("receiverAvailableEnd")
    public LocalDateTime getReceiverAvailableEnd() {
        return receiver_available_end_wsh;
    }

    @JsonGetter("pickupAddress")
    public String getPickupAddress() {
        return pickup_address_wsh;
    }

    @JsonGetter("pickupTime")
    public LocalDateTime getPickupTime() {
        return pickup_time_wsh;
    }

    @JsonGetter("startPhoto")
    public String getStartPhoto() {
        return start_photo_wsh;
    }

    @JsonGetter("totalAmount")
    public BigDecimal getTotalAmount() {
        return total_amount_wsh;
    }

    @JsonGetter("finalAmount")
    public BigDecimal getFinalAmount() {
        return final_amount_wsh;
    }
}
