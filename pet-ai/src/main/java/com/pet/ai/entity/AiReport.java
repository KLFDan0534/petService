package com.pet.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 报告实体，记录 AI 模型生成的护理建议和寄养总结报告。
 * <p>
 * 报告类型包括：care（护理建议）、final（寄养总结）。
 * 数据同时保存到 MySQL（主存储）和 Chroma 向量数据库（辅助检索）。
 */
@Getter
@Setter
@TableName("ai_report_wsh")
@Schema(description = "AI报告实体")
public class AiReport {
    @JsonProperty("id_wsh")
    @TableId(type = IdType.AUTO, value = "id_wsh")
    @Schema(description = "ID")
    private Long id_wsh;

    @JsonProperty("order_id_wsh")
    @TableField(value = "order_id_wsh")
    @Schema(description = "订单ID")
    private Long order_id_wsh;

    @JsonProperty("pet_id_wsh")
    @TableField(value = "pet_id_wsh")
    @Schema(description = "宠物ID")
    private Long pet_id_wsh;

    @JsonProperty("keeper_id_wsh")
    @TableField(value = "keeper_id_wsh")
    @Schema(description = "看护者ID")
    private Long keeper_id_wsh;

    @JsonProperty("content_wsh")
    @TableField(value = "content_wsh")
    @Schema(description = "内容")
    private String content_wsh;

    @JsonProperty("type_wsh")
    @TableField(value = "type_wsh")
    @Schema(description = "类型")
    private String type_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    @Schema(description = "逻辑删除标志")
    private Integer deleted_wsh;

    @JsonProperty("created_at_wsh")
    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
}
