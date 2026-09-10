package com.pet.membership.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Membership purchase or renewal order entity.
 * Records the order number, user, selected plan, payment amount and method,
 * order status lifecycle (pending -> paid / cancelled), and the membership
 * period this order covers. A plan snapshot is stored for historical reference.
 */
@Getter
@Setter
@TableName("membership_order_wsh")
@Schema(description = "会员购买与续费订单")
public class MembershipOrder {
    @TableId(value = "id_wsh", type = IdType.AUTO)
    private Long id_wsh;

    private String order_no_wsh;

    private Long user_id_wsh;

    private Long plan_id_wsh;

    private String plan_code_wsh;

    private BigDecimal amount_wsh;

    private String pay_method_wsh;

    private String status_wsh;

    private LocalDateTime paid_at_wsh;

    private LocalDateTime membership_start_at_wsh;

    private LocalDateTime membership_end_at_wsh;

    private String request_id_wsh;

    private String plan_snapshot_wsh;

    private String remark_wsh;

    @JsonIgnore
    @TableLogic
    private Integer deleted_wsh;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime created_at_wsh;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updated_at_wsh;
}
