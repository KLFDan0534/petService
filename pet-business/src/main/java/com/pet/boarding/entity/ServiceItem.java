package com.pet.boarding.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 服务项目（ServiceItem）实体，映射 pet_service_wsh 表。
 * <p>
 * 服务项目是商家提供的具体宠物服务，例如"标准洗浴 80元/次"、"全天寄养 100元/天"。
 * 每个服务项目归属于一个商家，并关联到一个服务分类（ServiceCategory）。
 * 分类的编码（code）会自动同步到服务项目的 type 字段，便于前端进行归类展示。
 * 服务项目可以独立启用/禁用，禁用后客户不可见。
 *
 * @author: wsh
 */
@Getter
@Setter
@TableName("pet_service_wsh")
@Schema(description = "服务项目实体")
public class ServiceItem {
    @TableId(value = "id_wsh", type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id_wsh;

    @TableField(value = "merchant_id_wsh")
    @Schema(description = "商家ID")
    private Long merchant_id_wsh;

    @TableField(value = "category_id_wsh")
    @Schema(description = "服务分类ID")
    private Long category_id_wsh;

    @TableField(value = "name_wsh")
    @Schema(description = "名称")
    private String name_wsh;

    @TableField(value = "type_wsh")
    @Schema(description = "类型")
    private String type_wsh;

    @TableField(value = "description_wsh")
    @Schema(description = "描述")
    private String description_wsh;

    @TableField(value = "price_wsh")
    @Schema(description = "价格")
    private BigDecimal price_wsh;

    @TableField(value = "unit_wsh")
    @Schema(description = "单位")
    private String unit_wsh;

    @TableField(value = "duration_minutes_wsh")
    @Schema(description = "单次服务时长（分钟）；day=1440，session/hour 自定义")
    private Integer duration_minutes_wsh;

    @TableField(value = "booking_mode_wsh")
    @Schema(description = "预约模式：date_range（按日期区间）/ slot（按时长槽位）")
    private String booking_mode_wsh;

    @TableField(value = "images_wsh")
    @Schema(description = "图片URL(逗号分隔)")
    private String images_wsh;

    @TableField(value = "status_wsh")
    @Schema(description = "状态")
    private Integer status_wsh;

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
