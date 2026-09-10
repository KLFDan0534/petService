package com.pet.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@TableName("pet_order_wsh")
@Schema(description = "宠物订单实体")
public class PetOrder {

    @TableId(value = "id_wsh", type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id_wsh;

    @TableField(value = "order_no_wsh")
    @Schema(description = "订单号")
    private String order_no_wsh;

    @TableField(value = "owner_id_wsh")
    @Schema(description = "宠物主用户ID")
    private Long owner_id_wsh;

    @TableField(value = "pet_id_wsh")
    @Schema(description = "宠物ID")
    private Long pet_id_wsh;

    @TableField(value = "keeper_id_wsh")
    @Schema(description = "看护者ID")
    private Long keeper_id_wsh;

    @TableField(value = "merchant_id_wsh")
    @Schema(description = "商家ID")
    private Long merchant_id_wsh;

    @TableField(value = "service_id_wsh")
    @Schema(description = "服务项目ID")
    private Long service_id_wsh;

    @TableField(value = "start_date_wsh")
    @Schema(description = "开始日期")
    private LocalDate start_date_wsh;

    @TableField(value = "end_date_wsh")
    @Schema(description = "结束日期")
    private LocalDate end_date_wsh;

    @TableField(value = "days_wsh")
    @Schema(description = "服务天数（day 模式=quantity；session/hour 模式固定为1的兼容投影）")
    private Integer days_wsh;

    @TableField(value = "price_per_day_wsh")
    @Schema(description = "每日价格（旧兼容字段，day 模式=unit_price；session/hour 模式为单单位单价）")
    private BigDecimal price_per_day_wsh;

    @TableField(value = "billing_unit_wsh")
    @Schema(description = "计费单位（下单时服务单位快照：day/session/hour）")
    private String billing_unit_wsh;

    @TableField(value = "quantity_wsh")
    @Schema(description = "计费数量（day=天数；session=1；hour=连续小时数）")
    private Integer quantity_wsh;

    @TableField(value = "unit_price_wsh")
    @Schema(description = "单价（下单时服务单价快照，计价权威）")
    private BigDecimal unit_price_wsh;

    @TableField(value = "duration_minutes_wsh")
    @Schema(description = "单次服务时长（分钟，下单时服务时长快照）")
    private Integer duration_minutes_wsh;

    @TableField(value = "total_amount_wsh")
    @Schema(description = "总金额")
    private BigDecimal total_amount_wsh;

    @TableField(value = "discount_wsh")
    @Schema(description = "折扣金额")
    private BigDecimal discount_wsh;

    @TableField(value = "coupon_id_wsh")
    @Schema(description = "优惠券ID")
    private Long coupon_id_wsh;

    @TableField(value = "coupon_template_id_wsh")
    @Schema(description = "优惠券模板ID")
    private Long coupon_template_id_wsh;

    @TableField(value = "coupon_discount_wsh")
    @Schema(description = "优惠券抵扣金额")
    private BigDecimal coupon_discount_wsh;

    @TableField(value = "membership_id_wsh")
    @Schema(description = "会员ID")
    private Long membership_id_wsh;

    @TableField(value = "membership_plan_id_wsh")
    @Schema(description = "会员套餐ID")
    private Long membership_plan_id_wsh;

    @TableField(value = "membership_discount_wsh")
    @Schema(description = "会员折扣金额")
    private BigDecimal membership_discount_wsh;

    @TableField(value = "membership_snapshot_wsh")
    @Schema(description = "会员权益快照")
    private String membership_snapshot_wsh;

    @TableField(value = "platform_subsidy_wsh")
    @Schema(description = "平台补贴金额")
    private BigDecimal platform_subsidy_wsh;

    @TableField(value = "settlement_amount_wsh")
    @Schema(description = "结算金额")
    private BigDecimal settlement_amount_wsh;

    @TableField(value = "promotion_snapshot_wsh")
    @Schema(description = "促销快照")
    private String promotion_snapshot_wsh;

    @TableField(value = "final_amount_wsh")
    @Schema(description = "最终支付金额")
    private BigDecimal final_amount_wsh;

    @TableField(value = "status_wsh")
    @Schema(description = "状态")
    private String status_wsh;

    @TableField(value = "handover_code_wsh")
    @Schema(description = "交接码")
    private String handover_code_wsh;

    @TableField(value = "delivery_address_wsh")
    @Schema(description = "配送地址")
    private String delivery_address_wsh;

    @TableField(value = "delivery_latitude_wsh")
    @Schema(description = "配送纬度")
    private BigDecimal delivery_latitude_wsh;

    @TableField(value = "delivery_longitude_wsh")
    @Schema(description = "配送经度")
    private BigDecimal delivery_longitude_wsh;

    @TableField(value = "delivery_location_source_wsh")
    @Schema(description = "配送位置来源")
    private String delivery_location_source_wsh;

    @TableField(value = "delivery_time_wsh")
    @Schema(description = "配送时间")
    private LocalDateTime delivery_time_wsh;

    @TableField(value = "receiver_available_start_wsh")
    @Schema(description = "接宠人空闲开始时间")
    private LocalDateTime receiver_available_start_wsh;

    @TableField(value = "receiver_available_end_wsh")
    @Schema(description = "接宠人空闲结束时间")
    private LocalDateTime receiver_available_end_wsh;

    @TableField(value = "emergency_contact_name_wsh")
    @Schema(description = "紧急联系人姓名")
    private String emergency_contact_name_wsh;

    @TableField(value = "emergency_contact_phone_wsh")
    @Schema(description = "紧急联系人电话")
    private String emergency_contact_phone_wsh;

    @TableField(value = "pickup_address_wsh")
    @Schema(description = "接宠地址")
    private String pickup_address_wsh;

    @TableField(value = "pickup_latitude_wsh")
    @Schema(description = "接宠纬度")
    private BigDecimal pickup_latitude_wsh;

    @TableField(value = "pickup_longitude_wsh")
    @Schema(description = "接宠经度")
    private BigDecimal pickup_longitude_wsh;

    @TableField(value = "pickup_location_source_wsh")
    @Schema(description = "接宠位置来源")
    private String pickup_location_source_wsh;

    @TableField(value = "pickup_time_wsh")
    @Schema(description = "接宠时间")
    private LocalDateTime pickup_time_wsh;

    @TableField(value = "delivered_at_wsh")
    @Schema(description = "送达时间")
    private LocalDateTime delivered_at_wsh;

    @TableField(value = "delivered_address_wsh")
    @Schema(description = "送达地址")
    private String delivered_address_wsh;

    @TableField(value = "delivered_latitude_wsh")
    @Schema(description = "送达纬度")
    private BigDecimal delivered_latitude_wsh;

    @TableField(value = "delivered_longitude_wsh")
    @Schema(description = "送达经度")
    private BigDecimal delivered_longitude_wsh;

    @TableField(value = "delivered_accuracy_wsh")
    @Schema(description = "送达定位精度")
    private BigDecimal delivered_accuracy_wsh;

    @TableField(value = "received_at_wsh")
    @Schema(description = "接宠时间")
    private LocalDateTime received_at_wsh;

    @TableField(value = "received_address_wsh")
    @Schema(description = "接收地址")
    private String received_address_wsh;

    @TableField(value = "received_latitude_wsh")
    @Schema(description = "接收纬度")
    private BigDecimal received_latitude_wsh;

    @TableField(value = "received_longitude_wsh")
    @Schema(description = "接收经度")
    private BigDecimal received_longitude_wsh;

    @TableField(value = "received_accuracy_wsh")
    @Schema(description = "接收定位精度")
    private BigDecimal received_accuracy_wsh;

    @TableField(value = "received_distance_m_wsh")
    @Schema(description = "接收距离")
    private BigDecimal received_distance_m_wsh;

    @TableField(value = "started_at_wsh")
    @Schema(description = "服务开始时间")
    private LocalDateTime started_at_wsh;

    @TableField(value = "start_photo_wsh")
    @Schema(description = "开始服务照片")
    private String start_photo_wsh;

    @TableField(value = "completed_at_wsh")
    @Schema(description = "服务完成时间")
    private LocalDateTime completed_at_wsh;

    @TableField(value = "final_report_generated_wsh")
    @Schema(description = "最终报告是否已生成")
    private Integer final_report_generated_wsh;    // 0:未生成 1:已生成

    @TableField(value = "remark_wsh")
    @Schema(description = "备注")
    private String remark_wsh;

    @TableField(exist = false)
    @Schema(description = "服务名称")
    private String service_name_wsh;

    @TableField(exist = false)
    @Schema(description = "宠物主姓名")
    private String owner_name_wsh;

    @TableField(exist = false)
    @Schema(description = "宠物名称")
    private String pet_name_wsh;

    @TableField(exist = false)
    @Schema(description = "看护者姓名")
    private String keeper_name_wsh;

    @TableField(exist = false)
    @Schema(description = "看护者电话")
    private String keeper_phone_wsh;

    @TableField(exist = false)
    @Schema(description = "看护者头像")
    private String keeper_avatar_wsh;

    @TableField(exist = false)
    @Schema(description = "商家名称")
    private String merchant_name_wsh;

    @TableField(exist = false)
    @Schema(description = "商家电话")
    private String merchant_phone_wsh;

    @TableField(exist = false)
    @Schema(description = "商家地址")
    private String merchant_address_wsh;

    @TableField(exist = false)
    @Schema(description = "服务描述")
    private String service_description_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    @Schema(description = "逻辑删除标志")
    private Integer deleted_wsh;

    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;

    @TableField(value = "updated_at_wsh", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updated_at_wsh;
}
