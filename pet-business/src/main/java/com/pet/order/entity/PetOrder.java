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

/**
 * 宠物订单实体
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Getter
@Setter
@TableName("pet_order_wsh")
public class PetOrder {

    // TODO 后期需要修改字段序列化,去掉Json注解,并且给前端使用当前数据的字段增加 _wsh后最

    @JsonProperty("id_wsh")
    @TableId(value = "id_wsh", type = IdType.AUTO)
    private Long id_wsh;

    @JsonProperty("order_no_wsh")
    @TableField(value = "order_no_wsh")
    private String order_no_wsh;

    @JsonProperty("owner_id_wsh")
    @TableField(value = "owner_id_wsh")
    private Long owner_id_wsh;

    @JsonProperty("pet_id_wsh")
    @TableField(value = "pet_id_wsh")
    private Long pet_id_wsh;

    @JsonProperty("keeper_id_wsh")
    @TableField(value = "keeper_id_wsh")
    private Long keeper_id_wsh;

    @JsonProperty("merchant_id_wsh")
    @TableField(value = "merchant_id_wsh")
    private Long merchant_id_wsh;

    @JsonProperty("service_id_wsh")
    @TableField(value = "service_id_wsh")
    private Long service_id_wsh;

    @JsonProperty("start_date_wsh")
    @JsonAlias({"startDate", "start_date"})
    @TableField(value = "start_date_wsh")
    private LocalDate start_date_wsh;

    @JsonProperty("end_date_wsh")
    @JsonAlias({"endDate", "end_date"})
    @TableField(value = "end_date_wsh")
    private LocalDate end_date_wsh;

    @JsonProperty("days_wsh")
    @TableField(value = "days_wsh")
    private Integer days_wsh;

    @JsonProperty("price_per_day_wsh")
    @TableField(value = "price_per_day_wsh")
    private BigDecimal price_per_day_wsh;

    @JsonProperty("total_amount_wsh")
    @TableField(value = "total_amount_wsh")
    private BigDecimal total_amount_wsh;

    @JsonProperty("discount_wsh")
    @TableField(value = "discount_wsh")
    private BigDecimal discount_wsh;

    @JsonProperty("final_amount_wsh")
    @TableField(value = "final_amount_wsh")
    private BigDecimal final_amount_wsh;

    @JsonProperty("status_wsh")
    @TableField(value = "status_wsh")
    private String status_wsh;

    @JsonProperty("handover_code_wsh")
    @JsonAlias({"handoverCode", "handover_code", "receiveCode", "receive_code"})
    @TableField(value = "handover_code_wsh")
    private String handover_code_wsh;

    @JsonProperty("delivery_address_wsh")
    @JsonAlias({"deliveryAddress", "delivery_address", "dropoffAddress", "dropoff_address"})
    @TableField(value = "delivery_address_wsh")
    private String delivery_address_wsh;

    @JsonProperty("delivery_time_wsh")
    @JsonAlias({"deliveryTime", "delivery_time", "dropoffTime", "dropoff_time"})
    @TableField(value = "delivery_time_wsh")
    private LocalDateTime delivery_time_wsh;

    @JsonProperty("receiver_available_start_wsh")
    @JsonAlias({"receiverAvailableStart", "receiver_available_start", "keeperAvailableStart", "keeper_available_start"})
    @TableField(value = "receiver_available_start_wsh")
    private LocalDateTime receiver_available_start_wsh;

    @JsonProperty("receiver_available_end_wsh")
    @JsonAlias({"receiverAvailableEnd", "receiver_available_end", "keeperAvailableEnd", "keeper_available_end"})
    @TableField(value = "receiver_available_end_wsh")
    private LocalDateTime receiver_available_end_wsh;

    @JsonProperty("pickup_address_wsh")
    @JsonAlias({"pickupAddress", "pickup_address", "returnAddress", "return_address"})
    @TableField(value = "pickup_address_wsh")
    private String pickup_address_wsh;

    @JsonProperty("pickup_time_wsh")
    @JsonAlias({"pickupTime", "pickup_time", "returnTime", "return_time"})
    @TableField(value = "pickup_time_wsh")
    private LocalDateTime pickup_time_wsh;

    @JsonProperty("delivered_at_wsh")
    @TableField(value = "delivered_at_wsh")
    private LocalDateTime delivered_at_wsh;

    @JsonProperty("received_at_wsh")
    @TableField(value = "received_at_wsh")
    private LocalDateTime received_at_wsh;

    @JsonProperty("started_at_wsh")
    @TableField(value = "started_at_wsh")
    private LocalDateTime started_at_wsh;

    @JsonProperty("start_photo_wsh")
    @JsonAlias({"startPhoto", "start_photo", "startPhotoUrl", "start_photo_url_wsh"})
    @TableField(value = "start_photo_wsh")
    private String start_photo_wsh;

    @JsonProperty("completed_at_wsh")
    @TableField(value = "completed_at_wsh")
    private LocalDateTime completed_at_wsh;

    @JsonProperty("final_report_generated_wsh")
    @TableField(value = "final_report_generated_wsh")
    private Integer final_report_generated_wsh;    // 0:未生成 1:已生成

    @JsonProperty("remark_wsh")
    @TableField(value = "remark_wsh")
    private String remark_wsh;

    @JsonProperty("service_name_wsh")
    @TableField(exist = false)
    private String service_name_wsh;

    @JsonProperty("owner_name_wsh")
    @TableField(exist = false)
    private String owner_name_wsh;

    @JsonProperty("pet_name_wsh")
    @TableField(exist = false)
    private String pet_name_wsh;

    @JsonProperty("keeper_name_wsh")
    @TableField(exist = false)
    private String keeper_name_wsh;

    @JsonProperty("keeper_phone_wsh")
    @TableField(exist = false)
    private String keeper_phone_wsh;

    @JsonProperty("keeper_avatar_wsh")
    @TableField(exist = false)
    private String keeper_avatar_wsh;

    @JsonProperty("merchant_name_wsh")
    @TableField(exist = false)
    private String merchant_name_wsh;

    @JsonProperty("merchant_phone_wsh")
    @TableField(exist = false)
    private String merchant_phone_wsh;

    @JsonProperty("merchant_address_wsh")
    @TableField(exist = false)
    private String merchant_address_wsh;

    @JsonProperty("service_description_wsh")
    @TableField(exist = false)
    private String service_description_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    private Integer deleted_wsh;

    @JsonProperty("created_at_wsh")
    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    private LocalDateTime created_at_wsh;

    @JsonProperty("updated_at_wsh")
    @TableField(value = "updated_at_wsh", fill = FieldFill.INSERT_UPDATE)
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
