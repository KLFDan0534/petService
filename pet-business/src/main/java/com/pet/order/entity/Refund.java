package com.pet.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款实体
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Getter
@Setter
@TableName("refund_wsh")
public class Refund {
    @JsonProperty("id_wsh")
    @TableId(value = "id_wsh", type = IdType.AUTO)
    private Long id_wsh;

    @JsonProperty("order_id_wsh")
    @TableField(value = "order_id_wsh")
    private Long order_id_wsh;

    @JsonProperty("order_no_wsh")
    @TableField(value = "order_no_wsh")
    private String order_no_wsh;

    @JsonProperty("amount_wsh")
    @TableField(value = "amount_wsh")
    private BigDecimal amount_wsh;

    @JsonProperty("reason_wsh")
    @TableField(value = "reason_wsh")
    private String reason_wsh;

    @JsonProperty("status_wsh")
    @TableField(value = "status_wsh")
    private String status_wsh;

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
