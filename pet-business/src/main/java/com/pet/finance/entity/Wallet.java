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
@TableName("wallet_wsh")
public class Wallet {
    @JsonProperty("id_wsh")
    @TableId(type = IdType.AUTO, value = "id_wsh")
    private Long idWsh;

    @JsonProperty("user_id_wsh")
    @TableField(value = "user_id_wsh", insertStrategy = FieldStrategy.NOT_NULL)
    private Long userIdWsh;

    @JsonProperty("balance_wsh")
    @TableField(value = "balance_wsh", insertStrategy = FieldStrategy.NOT_NULL)
    private BigDecimal balanceWsh;

    @JsonProperty("frozen_amount_wsh")
    @TableField(value = "frozen_amount_wsh", insertStrategy = FieldStrategy.NOT_NULL)
    private BigDecimal frozenAmountWsh;

    @JsonProperty("version_wsh")
    @Version
    @TableField(value = "version_wsh", insertStrategy = FieldStrategy.NOT_NULL)
    private Integer versionWsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    private Integer deletedWsh;

    @JsonProperty("created_at_wsh")
    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    private LocalDateTime createdAtWsh;

    @JsonProperty("updated_at_wsh")
    @TableField(value = "updated_at_wsh", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAtWsh;
}
