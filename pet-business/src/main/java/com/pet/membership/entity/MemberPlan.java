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
 * Member plan entity representing a purchasable membership tier.
 * Each plan defines a code, name, level, price, duration, discount rate,
 * and configuration for monthly coupons and other benefits.
 * Plans can be enabled or disabled for sale via the status field.
 */
@Getter
@Setter
@TableName("member_plan_wsh")
@Schema(description = "会员套餐")
public class MemberPlan {
    @TableId(value = "id_wsh", type = IdType.AUTO)
    private Long id_wsh;

    private String code_wsh;

    private String name_wsh;

    private Integer level_wsh;

    private BigDecimal price_wsh;

    private Integer duration_days_wsh;

    private BigDecimal discount_rate_wsh;

    private String monthly_coupon_config_wsh;

    private String benefit_config_wsh;

    private Integer status_wsh;

    private Integer sort_order_wsh;

    private String remark_wsh;

    @JsonIgnore
    @TableLogic
    private Integer deleted_wsh;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime created_at_wsh;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updated_at_wsh;
}
