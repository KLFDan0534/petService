package com.pet.order.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@TableName("tip_wsh")
@Schema(description = "小费实体")
public class Tip {
    @TableId(type = IdType.AUTO, value = "id_wsh")
    @Schema(description = "ID")
    private Long id_wsh;

    @NotNull(message = "orderId cannot be empty")
    @TableField(value = "order_id_wsh")
    @Schema(description = "订单ID")
    private Long order_id_wsh;

    @TableField(value = "from_user_id_wsh")
    @Schema(description = "发送方用户ID")
    private Long from_user_id_wsh;

    @TableField(value = "to_user_id_wsh")
    @Schema(description = "接收方用户ID")
    private Long to_user_id_wsh;

    @NotNull(message = "Tip amount cannot be empty")
    @DecimalMin(value = "0.01", message = "Tip amount must be greater than 0")
    @DecimalMax(value = "10000", message = "Tip amount cannot exceed 10000")
    @TableField(value = "amount_wsh")
    @Schema(description = "金额")
    private BigDecimal amount_wsh;

    @Size(max = 500, message = "Tip message cannot exceed 500 characters")
    @TableField(value = "message_wsh")
    @Schema(description = "留言")
    private String message_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    @Schema(description = "逻辑删除标志")
    private Integer deleted_wsh;

    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
}
