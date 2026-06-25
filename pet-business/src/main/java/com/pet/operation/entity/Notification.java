package com.pet.operation.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("notification_wsh")
public class Notification {
    @JsonProperty("id_wsh")
    @TableId(type = IdType.AUTO, value = "id_wsh")
    private Long id_wsh;

    @JsonProperty("user_id_wsh")
    @TableField(value = "user_id_wsh")
    private Long user_id_wsh;

    @JsonProperty("title_wsh")
    @TableField(value = "title_wsh")
    private String title_wsh;

    @JsonProperty("content_wsh")
    @TableField(value = "content_wsh")
    private String content_wsh;

    @JsonProperty("type_wsh")
    @TableField(value = "type_wsh")
    private String type_wsh;

    @JsonProperty("is_read_wsh")
    @TableField(value = "is_read_wsh")
    private Integer is_read_wsh;

    @JsonProperty("related_id_wsh")
    @TableField(value = "related_id_wsh")
    private Long related_id_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    private Integer deleted_wsh;

    @JsonProperty("created_at_wsh")
    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    private LocalDateTime created_at_wsh;
}
