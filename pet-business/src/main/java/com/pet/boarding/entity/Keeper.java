package com.pet.boarding.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 看护者（Keeper）实体，映射 keeper_wsh 表。
 * <p>
 * 看护者是实际提供宠物寄养服务的个体，隶属于某个商家（Merchant）。
 * 每个看护者与一个用户账号关联，系统通过角色（KEEPER）控制操作权限。
 * <p>
 * <b>状态流转：</b>
 * <ul>
 *   <li>入驻申请：KEEPER_PENDING（待审核）→ KEEPER_ACTIVE（已通过）/ KEEPER_REJECTED（已驳回）</li>
 *   <li>在职状态：KEEPER_ACTIVE（在线）↔ KEEPER_OFFLINE（离线）| KEEPER_BUSY（忙碌）</li>
 *   <li>离职状态：KEEPER_RESIGNED（辞职）/ KEEPER_TERMINATED（解雇）</li>
 * </ul>
 *
 * @author: wsh
 */
@Getter
@Setter
@TableName("keeper_wsh")
@Schema(description = "看护者实体")
public class Keeper {
    @TableId(value = "id_wsh", type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id_wsh;

    @TableField(value = "merchant_id_wsh")
    @Schema(description = "商家ID")
    private Long merchant_id_wsh;

    @TableField(value = "user_id_wsh")
    @Schema(description = "用户ID")
    private Long user_id_wsh;

    @TableField(value = "name_wsh")
    @Schema(description = "名称")
    private String name_wsh;

    @TableField(value = "phone_wsh")
    @Schema(description = "手机号")
    private String phone_wsh;

    @TableField(value = "avatar_wsh")
    @Schema(description = "头像URL")
    private String avatar_wsh;

    @TableField(value = "experience_years_wsh")
    @Schema(description = "从业经验年数")
    private Integer experience_years_wsh;

    @TableField(value = "rating_wsh")
    @Schema(description = "评分")
    private BigDecimal rating_wsh;

    @TableField(value = "completion_rate_wsh")
    @Schema(description = "完成率")
    private BigDecimal completion_rate_wsh;

    @TableField(value = "complaint_rate_wsh")
    @Schema(description = "投诉率")
    private BigDecimal complaint_rate_wsh;

    @TableField(value = "price_per_day_wsh")
    @Schema(description = "每日价格")
    private BigDecimal price_per_day_wsh;

    @TableField(value = "max_pets_wsh")
    @Schema(description = "最大可接待宠物数")
    private Integer max_pets_wsh;

    @TableField(value = "current_pets_wsh")
    @Schema(description = "当前已接待宠物数")
    private Integer current_pets_wsh;

    @TableField(value = "bio_wsh")
    @Schema(description = "个人简介")
    private String bio_wsh;

    @TableField(value = "status_wsh")
    @Schema(description = "状态")
    private Integer status_wsh;

    /**
     * 离线来源：0-店铺同步/系统 1-看护员主动离线。
     * <p>
     * 用于保留"关店同步看护员离线"语义的同时，禁止店铺开门同步覆盖看护员主动设置的离线状态
     * （主动离线看护员依然可接未来预约，仅不恢复实时在线展示）。
     */
    @TableField(value = "offline_source_wsh")
    @Schema(description = "离线来源: 0-店铺同步/系统 1-看护员主动离线")
    private Integer offline_source_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    @Schema(description = "逻辑删除标志")
    private Integer deleted_wsh;

    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;

    @TableField(value = "updated_at_wsh", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updated_at_wsh;
}
