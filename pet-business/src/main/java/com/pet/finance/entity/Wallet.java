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
 * 钱包实体，映射数据库表 wallet_wsh。
 * 每个用户拥有唯一钱包，记录当前余额和冻结金额。
 * 余额变动必须通过 AccountingService，以保证流水审计和幂等一致性。
 *
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Getter
@Setter
@TableName("wallet_wsh")
@Schema(description = "钱包实体")
public class Wallet {
    @JsonProperty("id_wsh")
    @TableId(type = IdType.AUTO, value = "id_wsh")
    @Schema(description = "钱包ID")
    private Long id_wsh;

    @JsonProperty("user_id_wsh")
    @TableField(value = "user_id_wsh", insertStrategy = FieldStrategy.NOT_NULL)
    @Schema(description = "用户ID")
    private Long user_id_wsh;

    @JsonProperty("balance_wsh")
    @TableField(value = "balance_wsh", insertStrategy = FieldStrategy.NOT_NULL)
    @Schema(description = "余额")
    private BigDecimal balance_wsh;

    @JsonProperty("frozen_amount_wsh")
    @TableField(value = "frozen_amount_wsh", insertStrategy = FieldStrategy.NOT_NULL)
    @Schema(description = "冻结金额")
    private BigDecimal frozen_amount_wsh;

    @JsonProperty("version_wsh")
    @Version
    @TableField(value = "version_wsh", insertStrategy = FieldStrategy.NOT_NULL)
    @Schema(description = "乐观锁版本号")
    private Integer version_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    @Schema(description = "逻辑删除标志")
    private Integer deleted_wsh;

    @JsonProperty("created_at_wsh")
    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;

    @JsonProperty("updated_at_wsh")
    @TableField(value = "updated_at_wsh", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updated_at_wsh;
}
