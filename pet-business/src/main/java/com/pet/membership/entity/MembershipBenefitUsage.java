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
 * Membership benefit usage record entity.
 * Tracks the lifecycle of a benefit applied to a business order:
 * locked (reserved) -> used (consumed) / released (cancelled).
 * Records the benefit type, amount, quantity, and a snapshot for auditing.
 */
@Getter
@Setter
@TableName("membership_benefit_usage_wsh")
@Schema(description = "会员权益使用记录")
public class MembershipBenefitUsage {
    @TableId(value = "id_wsh", type = IdType.AUTO)
    private Long id_wsh;

    private Long user_id_wsh;

    private Long membership_id_wsh;

    private Long plan_id_wsh;

    private String benefit_type_wsh;

    private String benefit_code_wsh;

    private String business_type_wsh;

    private String business_id_wsh;

    private BigDecimal amount_wsh;

    private Integer quantity_wsh;

    private String usage_status_wsh;

    private String request_id_wsh;

    private String usage_snapshot_wsh;

    private LocalDateTime used_at_wsh;

    @JsonIgnore
    @TableLogic
    private Integer deleted_wsh;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime created_at_wsh;
}
