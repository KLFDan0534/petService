package com.pet.membership.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("member_plan_wsh")
@Schema(description = "会员套餐")
public class MemberPlan {
    @JsonProperty("id_wsh")
    @TableId(value = "id_wsh", type = IdType.AUTO)
    private Long id_wsh;

    @JsonProperty("code_wsh")
    private String code_wsh;

    @JsonProperty("name_wsh")
    private String name_wsh;

    @JsonProperty("level_wsh")
    private Integer level_wsh;

    @JsonProperty("price_wsh")
    private BigDecimal price_wsh;

    @JsonProperty("duration_days_wsh")
    private Integer duration_days_wsh;

    @JsonProperty("discount_rate_wsh")
    private BigDecimal discount_rate_wsh;

    @JsonProperty("monthly_coupon_config_wsh")
    private String monthly_coupon_config_wsh;

    @JsonProperty("benefit_config_wsh")
    private String benefit_config_wsh;

    @JsonProperty("status_wsh")
    private Integer status_wsh;

    @JsonProperty("sort_order_wsh")
    private Integer sort_order_wsh;

    @JsonProperty("remark_wsh")
    private String remark_wsh;

    @JsonIgnore
    @TableLogic
    private Integer deleted_wsh;

    @JsonProperty("created_at_wsh")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime created_at_wsh;

    @JsonProperty("updated_at_wsh")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updated_at_wsh;
}
