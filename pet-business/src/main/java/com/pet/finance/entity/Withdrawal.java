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
 * 提现记录实体，映射数据库表 withdrawal_wsh。
 * 管理用户提现申请的完整生命周期：
 * pending（待审核）→ approved（审核通过）→ completed（已打款），
 * 或 pending → rejected（驳回）。
 * 提现时冻结用户余额，驳回时解冻，完成时从冻结区扣除。
 *
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Getter
@Setter
@TableName("withdrawal_wsh")
@Schema(description = "提现记录实体")
public class Withdrawal {
    @JsonProperty("id_wsh")
    @TableId(value = "id_wsh", type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id_wsh;

    @JsonProperty("user_id_wsh")
    @TableField(value = "user_id_wsh")
    @Schema(description = "用户ID")
    private Long user_id_wsh;

    @JsonProperty("amount_wsh")
    @TableField(value = "amount_wsh")
    @Schema(description = "金额")
    private BigDecimal amount_wsh;

    @JsonProperty("fee_wsh")
    @TableField(value = "fee_wsh")
    @Schema(description = "手续费")
    private BigDecimal fee_wsh;

    @JsonProperty("actual_amount_wsh")
    @TableField(value = "actual_amount_wsh")
    @Schema(description = "实际到账金额")
    private BigDecimal actual_amount_wsh;

    @JsonProperty("bank_name_wsh")
    @TableField(value = "bank_name_wsh")
    @Schema(description = "开户银行")
    private String bank_name_wsh;

    @JsonProperty("bank_card_wsh")
    @TableField(value = "bank_card_wsh")
    @Schema(description = "银行卡号")
    private String bank_card_wsh;

    @JsonProperty("account_name_wsh")
    @TableField(value = "account_name_wsh")
    @Schema(description = "开户姓名")
    private String account_name_wsh;

    @JsonProperty("status_wsh")
    @TableField(value = "status_wsh")
    @Schema(description = "状态")
    private String status_wsh;

    @JsonProperty("remark_wsh")
    @TableField(value = "remark_wsh")
    @Schema(description = "备注")
    private String remark_wsh;

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
