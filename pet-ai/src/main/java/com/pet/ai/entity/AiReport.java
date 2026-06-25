package com.pet.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("ai_report_wsh")
public class AiReport {
    @JsonProperty("id_wsh")
    @TableId(type = IdType.AUTO, value = "id_wsh")
    private Long id_wsh;

    @JsonProperty("order_id_wsh")
    @TableField(value = "order_id_wsh")
    private Long order_id_wsh;

    @JsonProperty("pet_id_wsh")
    @TableField(value = "pet_id_wsh")
    private Long pet_id_wsh;

    @JsonProperty("keeper_id_wsh")
    @TableField(value = "keeper_id_wsh")
    private Long keeper_id_wsh;

    @JsonProperty("content_wsh")
    @TableField(value = "content_wsh")
    private String content_wsh;

    @JsonProperty("type_wsh")
    @TableField(value = "type_wsh")
    private String type_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    private Integer deleted_wsh;

    @JsonProperty("created_at_wsh")
    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    private LocalDateTime created_at_wsh;
}
