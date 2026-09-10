package com.pet.customer.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 工单实体，记录用户提交的售后/客服工单信息。
 * <p>工单生命周期：pending（待处理）-> processing（处理中）-> resolved（已解决）-> closed（已关闭）。
 * 支持优先级、分类、分配处理人等功能。</p>
 */
@Getter
@Setter
@TableName("ticket_wsh")
@Schema(description = "工单实体")
public class Ticket {
    @TableId(value = "id_wsh", type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id_wsh;

    @TableField(value = "user_id_wsh")
    @Schema(description = "用户ID")
    private Long user_id_wsh;

    @TableField(value = "merchant_id_wsh")
    @Schema(description = "所属商家ID")
    private Long merchant_id_wsh;

    @TableField(value = "order_id_wsh")
    @Schema(description = "关联订单ID")
    private Long order_id_wsh;

    @TableField(value = "title_wsh")
    @Schema(description = "标题")
    private String title_wsh;

    @TableField(value = "content_wsh")
    @Schema(description = "内容")
    private String content_wsh;

    @TableField(value = "category_wsh")
    @Schema(description = "分类")
    private String category_wsh;

    @TableField(value = "priority_wsh")
    @Schema(description = "优先级")
    private String priority_wsh;

    @TableField(value = "status_wsh")
    @Schema(description = "状态")
    private String status_wsh;

    @TableField(value = "result_wsh")
    @Schema(description = "处理结果")
    private String result_wsh;

    @TableField(value = "assignee_id_wsh")
    @Schema(description = "处理人ID")
    private Long assignee_id_wsh;

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
