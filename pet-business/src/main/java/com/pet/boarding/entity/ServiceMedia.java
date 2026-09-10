package com.pet.boarding.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 服务产品图片实体，表示服务产品（pet_service_wsh）结构化的有序图片集合。
 * <p>
 * 每行引用一条产品用途的文件记录（file_record_wsh），通过 sort_order_wsh 保持
 * 稳定的展示顺序，is_cover_wsh 标记唯一封面。不为空图册时必须有且仅有一张封面，
 * 同一服务不能重复引用同一文件。
 */
@Getter
@Setter
@TableName("pet_service_media_wsh")
@Schema(description = "服务产品图片实体")
public class ServiceMedia {

    @TableId(type = IdType.AUTO, value = "id_wsh")
    @Schema(description = "图片ID")
    private Long id_wsh;

    @TableField(value = "service_id_wsh")
    @Schema(description = "服务产品ID")
    private Long service_id_wsh;

    @TableField(value = "file_id_wsh")
    @Schema(description = "文件记录ID")
    private Long file_id_wsh;

    @TableField(value = "sort_order_wsh")
    @Schema(description = "排序序号(0..N 连续)")
    private Integer sort_order_wsh;

    @TableField(value = "is_cover_wsh")
    @Schema(description = "是否封面: 0-否 1-是")
    private Integer is_cover_wsh;

    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;

    @TableField(value = "updated_at_wsh", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updated_at_wsh;
}