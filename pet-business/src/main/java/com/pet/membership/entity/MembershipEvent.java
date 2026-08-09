package com.pet.membership.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Membership lifecycle event entity that records significant events
 * in a user's membership journey, such as order creation, payment,
 * activation, renewal, and cancellation. Stores event type, status,
 * operator info, and a JSON snapshot for auditing.
 */
@Getter
@Setter
@TableName("membership_event_wsh")
@Schema(description = "会员生命周期事件")
public class MembershipEvent {
    @JsonProperty("id_wsh")
    @TableId(value = "id_wsh", type = IdType.AUTO)
    private Long id_wsh;

    @JsonProperty("user_id_wsh")
    private Long user_id_wsh;

    @JsonProperty("membership_id_wsh")
    private Long membership_id_wsh;

    @JsonProperty("membership_order_id_wsh")
    private Long membership_order_id_wsh;

    @JsonProperty("event_type_wsh")
    private String event_type_wsh;

    @JsonProperty("event_status_wsh")
    private String event_status_wsh;

    @JsonProperty("operator_id_wsh")
    private Long operator_id_wsh;

    @JsonProperty("message_wsh")
    private String message_wsh;

    @JsonProperty("event_snapshot_wsh")
    private String event_snapshot_wsh;

    @JsonProperty("created_at_wsh")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime created_at_wsh;
}
