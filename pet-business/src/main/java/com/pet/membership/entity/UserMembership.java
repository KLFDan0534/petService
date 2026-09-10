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

import java.time.LocalDateTime;

/**
 * User membership entity representing the current membership status of a user.
 * Tracks the active plan, validity period (started_at / expires_at), auto-renew flag,
 * source of activation, and a benefit snapshot of the plan at time of purchase.
 */
@Getter
@Setter
@TableName("user_membership_wsh")
@Schema(description = "用户当前会员状态")
public class UserMembership {
    @TableId(value = "id_wsh", type = IdType.AUTO)
    private Long id_wsh;

    private Long user_id_wsh;

    private Long plan_id_wsh;

    private String plan_code_wsh;

    private Integer level_wsh;

    private String status_wsh;

    private LocalDateTime started_at_wsh;

    private LocalDateTime expires_at_wsh;

    private Integer auto_renew_wsh;

    private String source_wsh;

    private Long last_order_id_wsh;

    private String benefit_snapshot_wsh;

    @JsonIgnore
    @TableLogic
    private Integer deleted_wsh;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime created_at_wsh;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updated_at_wsh;
}
