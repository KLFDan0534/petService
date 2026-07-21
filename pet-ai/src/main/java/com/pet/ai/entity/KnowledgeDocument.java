package com.pet.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@TableName("knowledge_document_wsh")
@Schema(description = "知识文档实体")
public class KnowledgeDocument {
    @JsonProperty("id_wsh")
    @TableId(value = "id_wsh", type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id_wsh;

    @NotBlank(message = "标题不能为空")
    @JsonProperty("title_wsh")
    @TableField(value = "title_wsh")
    @Schema(description = "标题")
    private String title_wsh;

    @Size(max = 10000, message = "内容不能超过 10000 字符")
    @JsonProperty("content_wsh")
    @TableField(value = "content_wsh")
    @Schema(description = "内容")
    private String content_wsh;

    @JsonProperty("category_wsh")
    @TableField(value = "category_wsh")
    @Schema(description = "分类")
    private String category_wsh;

    @JsonProperty("source_type_wsh")
    @TableField(value = "source_type_wsh")
    @Schema(description = "来源类型")
    private String source_type_wsh;

    @JsonProperty("source_path_wsh")
    @TableField(value = "source_path_wsh")
    @Schema(description = "来源路径")
    private String source_path_wsh;

    @JsonProperty("word_count_wsh")
    @TableField(value = "word_count_wsh")
    @Schema(description = "字数")
    private Integer word_count_wsh;

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
