package com.pet.customer.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("ticket_wsh")
public class Ticket {
    @JsonProperty("id_wsh")
    @TableId(value = "id_wsh", type = IdType.AUTO)
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

    @JsonProperty("category_wsh")
    @TableField(value = "category_wsh")
    private String category_wsh;

    @JsonProperty("priority_wsh")
    @TableField(value = "priority_wsh")
    private String priority_wsh;

    @JsonProperty("status_wsh")
    @TableField(value = "status_wsh")
    private String status_wsh;

    @JsonProperty("assignee_id_wsh")
    @TableField(value = "assignee_id_wsh")
    private Long assignee_id_wsh;

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
