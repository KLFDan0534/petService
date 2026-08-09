package com.pet.marketing.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户优惠券实体，记录发放给用户的优惠券实例。
 * <p>状态流转：available（可用）-> locked（已锁定，下单时暂扣）-> used（已使用）；
 * 或从 locked 回到 available（订单取消时释放）。
 * 每个优惠券实例关联一个模板，继承其优惠规则。</p>
 */
@Getter
@Setter
@TableName("user_coupon_wsh")
@Schema(description = "用户优惠券实体")
public class UserCoupon {
    @TableId(value = "id_wsh", type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id_wsh;

    @Schema(description = "优惠券模板ID")
    private Long template_id_wsh;

    @Schema(description = "用户ID")
    private Long user_id_wsh;

    @Schema(description = "状态")
    private String status_wsh;

    @Schema(description = "来源")
    private String source_wsh;

    @Schema(description = "订单ID")
    private Long order_id_wsh;

    @Schema(description = "订单号")
    private String order_no_wsh;

    @Schema(description = "优惠金额")
    private BigDecimal discount_amount_wsh;

    @Schema(description = "锁定时间")
    private LocalDateTime locked_at_wsh;

    @Schema(description = "使用时间")
    private LocalDateTime used_at_wsh;

    @Schema(description = "过期时间")
    private LocalDateTime expire_at_wsh;

    @JsonIgnore
    @TableLogic
    @Schema(description = "逻辑删除标志")
    private Integer deleted_wsh;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updated_at_wsh;
}
