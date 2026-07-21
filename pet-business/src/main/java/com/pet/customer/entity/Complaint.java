package com.pet.customer.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@TableName("complaint_wsh")
@Schema(description = "投诉实体")
public class Complaint {
    @JsonProperty("id_wsh")
    @TableId(type = IdType.AUTO, value = "id_wsh")
    @Schema(description = "ID")
    private Long id_wsh;

    @JsonProperty("order_id_wsh")
    @TableField(value = "order_id_wsh")
    @Schema(description = "订单ID")
    private Long order_id_wsh;

    @JsonProperty("merchant_id_wsh")
    @TableField(value = "merchant_id_wsh")
    @Schema(description = "所属商家ID")
    private Long merchant_id_wsh;

    @JsonProperty("owner_id_wsh")
    @TableField(value = "owner_id_wsh")
    @Schema(description = "宠物主用户ID")
    private Long owner_id_wsh;

    @JsonProperty("target_id_wsh")
    @TableField(value = "target_id_wsh")
    @Schema(description = "被投诉/目标对象ID")
    private Long target_id_wsh;

    @JsonProperty("target_type_wsh")
    @TableField(value = "target_type_wsh")
    @Schema(description = "被投诉/目标对象类型")
    private String target_type_wsh;

    @JsonProperty("title_wsh")
    @TableField(value = "title_wsh")
    @Schema(description = "标题")
    private String title_wsh;

    @JsonProperty("content_wsh")
    @TableField(value = "content_wsh")
    @Schema(description = "内容")
    private String content_wsh;

    @JsonProperty("images_wsh")
    @TableField(value = "images_wsh")
    @Schema(description = "图片URL(逗号分隔)")
    private String images_wsh;

    @JsonProperty("owner_name_wsh")
    @TableField(exist = false)
    @Schema(description = "投诉人姓名")
    private String owner_name_wsh;

    @JsonProperty("status_wsh")
    @TableField(value = "status_wsh")
    @Schema(description = "状态")
    private String status_wsh;

    @JsonProperty("result_wsh")
    @TableField(value = "result_wsh")
    @Schema(description = "处理结果")
    private String result_wsh;

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
