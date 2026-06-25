package com.pet.customer.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("complaint_wsh")
public class Complaint {
    @JsonProperty("id_wsh")
    @TableId(type = IdType.AUTO, value = "id_wsh")
    private Long id_wsh;

    @JsonProperty("order_id_wsh")
    @TableField(value = "order_id_wsh")
    private Long order_id_wsh;

    @JsonProperty("owner_id_wsh")
    @TableField(value = "owner_id_wsh")
    private Long owner_id_wsh;

    @JsonProperty("target_id_wsh")
    @TableField(value = "target_id_wsh")
    private Long target_id_wsh;

    @JsonProperty("target_type_wsh")
    @TableField(value = "target_type_wsh")
    private String target_type_wsh;

    @JsonProperty("title_wsh")
    @TableField(value = "title_wsh")
    private String title_wsh;

    @JsonProperty("content_wsh")
    @TableField(value = "content_wsh")
    private String content_wsh;

    @JsonProperty("images_wsh")
    @TableField(value = "images_wsh")
    private String images_wsh;

    @JsonProperty("owner_name_wsh")
    @TableField(exist = false)
    private String owner_name_wsh;

    @JsonProperty("status_wsh")
    @TableField(value = "status_wsh")
    private String status_wsh;

    @JsonProperty("result_wsh")
    @TableField(value = "result_wsh")
    private String result_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    private Integer deleted_wsh;

    @JsonProperty("created_at_wsh")
    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    private LocalDateTime created_at_wsh;

    @JsonProperty("updated_at_wsh")
    @TableField(value = "updated_at_wsh", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updated_at_wsh;
}
