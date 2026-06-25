package com.pet.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("knowledge_document_wsh")
public class KnowledgeDocument {
    @JsonProperty("id_wsh")
    @TableId(value = "id_wsh", type = IdType.AUTO)
    private Long id_wsh;

    @NotBlank(message = "标题不能为空")
    @JsonProperty("title_wsh")
    @TableField(value = "title_wsh")
    private String title_wsh;

    @Size(max = 10000, message = "内容不能超过 10000 字符")
    @JsonProperty("content_wsh")
    @TableField(value = "content_wsh")
    private String content_wsh;

    @JsonProperty("category_wsh")
    @TableField(value = "category_wsh")
    private String category_wsh;

    @JsonProperty("source_type_wsh")
    @TableField(value = "source_type_wsh")
    private String source_type_wsh;

    @JsonProperty("source_path_wsh")
    @TableField(value = "source_path_wsh")
    private String source_path_wsh;

    @JsonProperty("word_count_wsh")
    @TableField(value = "word_count_wsh")
    private Integer word_count_wsh;

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
