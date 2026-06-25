package com.pet.operation.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("favorite_wsh")
public class Favorite {
    @JsonProperty("id_wsh")
    @TableId(type = IdType.AUTO, value = "id_wsh")
    private Long id_wsh;

    @JsonProperty("user_id_wsh")
    @TableField(value = "user_id_wsh")
    private Long user_id_wsh;

    @JsonProperty("target_id_wsh")
    @TableField(value = "target_id_wsh")
    private Long target_id_wsh;

    @JsonProperty("target_type_wsh")
    @TableField(value = "target_type_wsh")
    private String target_type_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    private Integer deleted_wsh;

    @JsonProperty("created_at_wsh")
    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    private LocalDateTime created_at_wsh;
}
