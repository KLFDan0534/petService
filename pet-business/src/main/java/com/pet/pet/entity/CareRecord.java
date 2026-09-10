package com.pet.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 护理记录实体，映射数据库表 care_record_wsh。
 * 记录宠物在寄养服务过程中的护理操作（喂养、遛狗、喂药等）。
 * 关联订单、宠物和看护者，包含护理类型、内容描述和图片。
 * 是后续生成护理报告和结算的重要依据。
 *
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Getter
@Setter
@TableName("care_record_wsh")
@Schema(description = "护理记录实体")
public class CareRecord {
    @TableId(type = IdType.AUTO, value = "id_wsh")
    @Schema(description = "ID")
    private Long id_wsh;

    @TableField(value = "order_id_wsh")
    @Schema(description = "订单ID")
    private Long order_id_wsh;

    @TableField(value = "pet_id_wsh")
    @Schema(description = "宠物ID")
    private Long pet_id_wsh;

    @TableField(value = "keeper_id_wsh")
    @Schema(description = "看护者ID")
    private Long keeper_id_wsh;

    @TableField(value = "type_wsh")
    @Schema(description = "类型")
    private String type_wsh;

    @TableField(value = "content_wsh")
    @Schema(description = "内容")
    private String content_wsh;

    @TableField(value = "images_wsh")
    @Schema(description = "图片URL(逗号分隔)")
    private String images_wsh;

    @TableField(value = "record_time_wsh")
    @Schema(description = "护理记录时间")
    private LocalDateTime record_time_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    @Schema(description = "逻辑删除标志")
    private Integer deleted_wsh;

    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
}
