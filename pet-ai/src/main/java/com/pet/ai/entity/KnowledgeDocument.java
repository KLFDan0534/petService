package com.pet.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 知识文档实体，存储在 RAG 知识库中的文档条目。
 * <p>
 * 文档内容存储在 Chroma 向量数据库中，MySQL 表仅作为冗余备份。
 * 支持 .txt 和 .docx 文件导入，内容经 HTML 转义后存储。
 */
@Getter
@Setter
@TableName("knowledge_document_wsh")
@Schema(description = "知识文档实体")
public class KnowledgeDocument {
    @TableId(value = "id_wsh", type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id_wsh;

    @NotBlank(message = "标题不能为空")
    @TableField(value = "title_wsh")
    @Schema(description = "标题")
    private String title_wsh;

    @Size(max = 10000, message = "内容不能超过 10000 字符")
    @TableField(value = "content_wsh")
    @Schema(description = "内容")
    private String content_wsh;

    @TableField(value = "category_wsh")
    @Schema(description = "分类")
    private String category_wsh;

    @TableField(value = "source_type_wsh")
    @Schema(description = "来源类型")
    private String source_type_wsh;

    @TableField(value = "source_path_wsh")
    @Schema(description = "来源路径")
    private String source_path_wsh;

    @TableField(value = "word_count_wsh")
    @Schema(description = "字数")
    private Integer word_count_wsh;

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
