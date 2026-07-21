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

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("user_membership_wsh")
@Schema(description = "用户当前会员状态")
public class UserMembership {
    @JsonProperty("id_wsh")
    @TableId(value = "id_wsh", type = IdType.AUTO)
    private Long id_wsh;

    @JsonProperty("user_id_wsh")
    private Long user_id_wsh;

    @JsonProperty("plan_id_wsh")
    private Long plan_id_wsh;

    @JsonProperty("plan_code_wsh")
    private String plan_code_wsh;

    @JsonProperty("level_wsh")
    private Integer level_wsh;

    @JsonProperty("status_wsh")
    private String status_wsh;

    @JsonProperty("started_at_wsh")
    private LocalDateTime started_at_wsh;

    @JsonProperty("expires_at_wsh")
    private LocalDateTime expires_at_wsh;

    @JsonProperty("auto_renew_wsh")
    private Integer auto_renew_wsh;

    @JsonProperty("source_wsh")
    private String source_wsh;

    @JsonProperty("last_order_id_wsh")
    private Long last_order_id_wsh;

    @JsonProperty("benefit_snapshot_wsh")
    private String benefit_snapshot_wsh;

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
