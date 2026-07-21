package com.pet.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 退款实体
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Getter
@Setter
@TableName("refund_wsh")
@Schema(description = "退款实体")
public class Refund {
    @JsonProperty("id_wsh")
    @TableId(value = "id_wsh", type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id_wsh;

    @JsonProperty("order_id_wsh")
    @TableField(value = "order_id_wsh")
    @Schema(description = "订单ID")
    private Long order_id_wsh;

    @JsonProperty("order_no_wsh")
    @TableField(value = "order_no_wsh")
    @Schema(description = "订单号")
    private String order_no_wsh;

    @JsonProperty("amount_wsh")
    @TableField(value = "amount_wsh")
    @Schema(description = "金额")
    private BigDecimal amount_wsh;

    @JsonProperty("reason_wsh")
    @TableField(value = "reason_wsh")
    @Schema(description = "原因")
    private String reason_wsh;

    @JsonProperty("status_wsh")
    @TableField(value = "status_wsh")
    @Schema(description = "状态")
    private String status_wsh;

    @JsonProperty("order_status_before_refund_wsh")
    @TableField(value = "order_status_before_refund_wsh")
    @Schema(description = "退款前订单状态")
    private String order_status_before_refund_wsh;

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

    @JsonGetter("orderId")
    public Long getOrderId() {
        return order_id_wsh;
    }

    @JsonGetter("orderNo")
    public String getOrderNo() {
        return order_no_wsh;
    }

    @JsonGetter("amount")
    public BigDecimal getAmount() {
        return amount_wsh;
    }

    @JsonGetter("reason")
    public String getReason() {
        return reason_wsh;
    }

    @JsonGetter("status")
    public String getStatus() {
        return status_wsh;
    }
}
