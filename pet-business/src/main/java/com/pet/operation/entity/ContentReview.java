package com.pet.operation.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("content_review_wsh")
public class ContentReview {
    @JsonProperty("id_wsh")
    @TableId(value = "id_wsh", type = IdType.AUTO)
    private Long id_wsh;

    @JsonProperty("target_type_wsh")
    @TableField(value = "target_type_wsh")
    private String target_type_wsh;

    @JsonProperty("target_id_wsh")
    @TableField(value = "target_id_wsh")
    private Long target_id_wsh;

    @JsonProperty("reporter_id_wsh")
    @TableField(value = "reporter_id_wsh")
    private Long reporter_id_wsh;

    @JsonProperty("reason_wsh")
    @TableField(value = "reason_wsh")
    private String reason_wsh;

    @JsonProperty("status_wsh")
    @TableField(value = "status_wsh")
    private String status_wsh;

    @JsonProperty("reviewer_id_wsh")
    @TableField(value = "reviewer_id_wsh")
    private Long reviewer_id_wsh;

    @JsonProperty("review_remark_wsh")
    @TableField(value = "review_remark_wsh")
    private String review_remark_wsh;

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
