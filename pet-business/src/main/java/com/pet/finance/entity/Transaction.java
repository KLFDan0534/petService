package com.pet.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 交易记录实体，映射数据库表 wallet_transaction_wsh。
 * 记录每次钱包余额或冻结金额变动的完整快照（变动前/后），
 * 包含交易类型、方向、金额、业务关联信息和幂等请求 ID。
 * 是资金审计和对账的核心数据源。
 *
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Getter
@Setter
@TableName("wallet_transaction_wsh")
@Schema(description = "交易记录实体")
public class Transaction {
    @JsonProperty("id_wsh")
    @TableId(value = "id_wsh", type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id_wsh;

    @JsonProperty("wallet_id_wsh")
    @TableField(value = "wallet_id_wsh")
    @Schema(description = "钱包ID")
    private Long wallet_id_wsh;

    @JsonProperty("user_id_wsh")
    @TableField(value = "user_id_wsh")
    @Schema(description = "用户ID")
    private Long user_id_wsh;

    @JsonProperty("type_wsh")
    @TableField(value = "type_wsh")
    @Schema(description = "类型")
    private String type_wsh;

    @JsonProperty("amount_wsh")
    @TableField(value = "amount_wsh")
    @Schema(description = "金额")
    private BigDecimal amount_wsh;

    @JsonProperty("balance_before_wsh")
    @TableField(value = "balance_before_wsh")
    @Schema(description = "变动前余额")
    private BigDecimal balance_before_wsh;

    @JsonProperty("balance_after_wsh")
    @TableField(value = "balance_after_wsh")
    @Schema(description = "变动后余额")
    private BigDecimal balance_after_wsh;

    @JsonProperty("frozen_before_wsh")
    @TableField(value = "frozen_before_wsh")
    @Schema(description = "变动前冻结金额")
    private BigDecimal frozen_before_wsh;

    @JsonProperty("frozen_after_wsh")
    @TableField(value = "frozen_after_wsh")
    @Schema(description = "变动后冻结金额")
    private BigDecimal frozen_after_wsh;

    @JsonProperty("direction_wsh")
    @TableField(value = "direction_wsh")
    @Schema(description = "交易方向")
    private String direction_wsh;

    @JsonProperty("status_wsh")
    @TableField(value = "status_wsh")
    @Schema(description = "交易状态")
    private String status_wsh;

    @JsonProperty("business_type_wsh")
    @TableField(value = "business_type_wsh")
    @Schema(description = "业务类型")
    private String business_type_wsh;

    @JsonProperty("business_id_wsh")
    @TableField(value = "business_id_wsh")
    @Schema(description = "业务ID")
    private String business_id_wsh;

    @JsonProperty("request_id_wsh")
    @TableField(value = "request_id_wsh")
    @Schema(description = "幂等请求ID")
    private String request_id_wsh;

    @JsonProperty("order_id_wsh")
    @TableField(value = "order_id_wsh")
    @Schema(description = "订单ID")
    private Long order_id_wsh;

    @JsonProperty("description_wsh")
    @TableField(value = "description_wsh")
    @Schema(description = "描述")
    private String description_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    @Schema(description = "逻辑删除标志")
    private Integer deleted_wsh;

    @JsonProperty("created_at_wsh")
    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
}
