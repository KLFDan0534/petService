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

@Getter
@Setter
@TableName("coupon_template_wsh")
@Schema(description = "优惠券模板实体")
public class CouponTemplate {
    @TableId(value = "id_wsh", type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id_wsh;

    @Schema(description = "名称")
    private String name_wsh;

    @Schema(description = "类型")
    private String type_wsh;

    @Schema(description = "使用门槛金额")
    private BigDecimal threshold_amount_wsh;

    @Schema(description = "优惠金额")
    private BigDecimal discount_amount_wsh;

    @Schema(description = "折扣率")
    private BigDecimal discount_rate_wsh;

    @Schema(description = "最大优惠金额")
    private BigDecimal max_discount_amount_wsh;

    @Schema(description = "总发行量")
    private Integer total_quantity_wsh;

    @Schema(description = "已发行量")
    private Integer issued_quantity_wsh;

    @Schema(description = "每人限领数量")
    private Integer per_user_limit_wsh;

    @Schema(description = "有效期开始时间")
    private LocalDateTime valid_from_wsh;

    @Schema(description = "有效期结束时间")
    private LocalDateTime valid_to_wsh;

    @Schema(description = "状态")
    private Integer status_wsh;

    @Schema(description = "适用范围类型")
    private String scope_type_wsh;

    @Schema(description = "商家ID")
    private Long merchant_id_wsh;

    @Schema(description = "创建人")
    private Long created_by_wsh;

    @Schema(description = "备注")
    private String remark_wsh;

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
