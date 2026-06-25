package com.pet.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("wallet_transaction_wsh")
public class Transaction {
    @JsonProperty("id_wsh")
    @TableId(value = "id_wsh", type = IdType.AUTO)
    private Long id_wsh;

    @JsonProperty("wallet_id_wsh")
    @TableField(value = "wallet_id_wsh")
    private Long wallet_id_wsh;

    @JsonProperty("user_id_wsh")
    @TableField(value = "user_id_wsh")
    private Long user_id_wsh;

    @JsonProperty("type_wsh")
    @TableField(value = "type_wsh")
    private String type_wsh;

    @JsonProperty("amount_wsh")
    @TableField(value = "amount_wsh")
    private BigDecimal amount_wsh;

    @JsonProperty("balance_after_wsh")
    @TableField(value = "balance_after_wsh")
    private BigDecimal balance_after_wsh;

    @JsonProperty("order_id_wsh")
    @TableField(value = "order_id_wsh")
    private Long order_id_wsh;

    @JsonProperty("description_wsh")
    @TableField(value = "description_wsh")
    private String description_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    private Integer deleted_wsh;

    @JsonProperty("created_at_wsh")
    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    private LocalDateTime created_at_wsh;
}
