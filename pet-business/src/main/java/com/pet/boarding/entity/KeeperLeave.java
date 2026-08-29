package com.pet.boarding.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 看护者休假（KeeperLeave）实体，映射 keeper_leave_wsh 表。
 * <p>
 * 由商家为其旗下的看护者设置休假安排。休假以日期范围表示（开始日期 ~ 结束日期），
 * 支持跨天休假。休假期间看护者不能签到打卡和接单。
 * 系统提供日期重叠检测，防止同一看护者在同一时间段被重复设置休假。
 * 记录中保存了创建人（商家用户ID），便于审计。
 */
@Getter
@Setter
@TableName("keeper_leave_wsh")
@Schema(description = "看护者请假实体")
public class KeeperLeave {
    @TableId(value = "id_wsh", type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id_wsh;

    @Schema(description = "看护者ID")
    private Long keeper_id_wsh;

    @Schema(description = "商家ID")
    private Long merchant_id_wsh;

    @Schema(description = "开始日期")
    private LocalDate start_date_wsh;

    @Schema(description = "结束日期")
    private LocalDate end_date_wsh;

    @Schema(description = "原因")
    private String reason_wsh;

    @Schema(description = "审批状态 pending/approved/rejected")
    private String status_wsh;

    @Schema(description = "创建人")
    private Long created_by_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    @Schema(description = "逻辑删除标志")
    private Integer deleted_wsh;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updated_at_wsh;
}
