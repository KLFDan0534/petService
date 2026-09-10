package com.pet.operation.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 内容审核实体，记录用户对平台内容的举报及管理员的审核处理结果。
 * <p>
 * 审核流程：用户举报 -> 状态 PENDING -> 管理员审核通过(APPROVED) 或驳回(REJECTED)。
 * 审核记录包含举报人、审核人、原因和备注信息。
 */
@Getter
@Setter
@TableName("content_review_wsh")
@Schema(description = "内容审核实体")
public class ContentReview {
    @TableId(value = "id_wsh", type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id_wsh;

    @TableField(value = "target_type_wsh")
    @Schema(description = "被投诉/目标对象类型")
    private String target_type_wsh;

    @TableField(value = "target_id_wsh")
    @Schema(description = "被投诉/目标对象ID")
    private Long target_id_wsh;

    @TableField(value = "reporter_id_wsh")
    @Schema(description = "举报人ID")
    private Long reporter_id_wsh;

    @TableField(value = "reason_wsh")
    @Schema(description = "原因")
    private String reason_wsh;

    @TableField(value = "status_wsh")
    @Schema(description = "状态")
    private String status_wsh;

    @TableField(value = "reviewer_id_wsh")
    @Schema(description = "审核人ID")
    private Long reviewer_id_wsh;

    @TableField(value = "review_remark_wsh")
    @Schema(description = "审核备注")
    private String review_remark_wsh;

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
