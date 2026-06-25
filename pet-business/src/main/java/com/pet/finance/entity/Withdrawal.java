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
@TableName("withdrawal_wsh")
public class Withdrawal {
    @JsonProperty("id_wsh")
    @TableId(value = "id_wsh", type = IdType.AUTO)
    private Long id_wsh;

    @JsonProperty("user_id_wsh")
    @TableField(value = "user_id_wsh")
    private Long user_id_wsh;

    @JsonProperty("amount_wsh")
    @TableField(value = "amount_wsh")
    private BigDecimal amount_wsh;

    @JsonProperty("fee_wsh")
    @TableField(value = "fee_wsh")
    private BigDecimal fee_wsh;

    @JsonProperty("actual_amount_wsh")
    @TableField(value = "actual_amount_wsh")
    private BigDecimal actual_amount_wsh;

    @JsonProperty("bank_name_wsh")
    @TableField(value = "bank_name_wsh")
    private String bank_name_wsh;

    @JsonProperty("bank_card_wsh")
    @TableField(value = "bank_card_wsh")
    private String bank_card_wsh;

    @JsonProperty("account_name_wsh")
    @TableField(value = "account_name_wsh")
    private String account_name_wsh;

    @JsonProperty("status_wsh")
    @TableField(value = "status_wsh")
    private String status_wsh;

    @JsonProperty("remark_wsh")
    @TableField(value = "remark_wsh")
    private String remark_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    private Integer deleted_wsh;

    @JsonProperty("created_at_wsh")
    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    private LocalDateTime created_at_wsh;

    @JsonProperty("updated_at_wsh")
    @TableField(value = "updated_at_wsh", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updated_at_wsh;
}
