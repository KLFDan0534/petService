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
@TableName("membership_benefit_usage_wsh")
@Schema(description = "会员权益使用记录")
public class MembershipBenefitUsage {
    @JsonProperty("id_wsh")
    @TableId(value = "id_wsh", type = IdType.AUTO)
    private Long id_wsh;

    @JsonProperty("user_id_wsh")
    private Long user_id_wsh;

    @JsonProperty("membership_id_wsh")
    private Long membership_id_wsh;

    @JsonProperty("plan_id_wsh")
    private Long plan_id_wsh;

    @JsonProperty("benefit_type_wsh")
    private String benefit_type_wsh;

    @JsonProperty("benefit_code_wsh")
    private String benefit_code_wsh;

    @JsonProperty("business_type_wsh")
    private String business_type_wsh;

    @JsonProperty("business_id_wsh")
    private String business_id_wsh;

    @JsonProperty("amount_wsh")
    private BigDecimal amount_wsh;

    @JsonProperty("quantity_wsh")
    private Integer quantity_wsh;

    @JsonProperty("usage_status_wsh")
    private String usage_status_wsh;

    @JsonProperty("request_id_wsh")
    private String request_id_wsh;

    @JsonProperty("usage_snapshot_wsh")
    private String usage_snapshot_wsh;

    @JsonProperty("used_at_wsh")
    private LocalDateTime used_at_wsh;

    @JsonIgnore
    @TableLogic
    private Integer deleted_wsh;

    @JsonProperty("created_at_wsh")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime created_at_wsh;
}
