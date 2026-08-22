package com.pet.ai.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 知识库文档实体，映射 knowledge_document_wsh 表。
 * <p>存储 AI 客服检索的领域知识（寄养规则、退款规则、投诉规则、护理指南等），
 * AI 回复前先检索该表内容拼入提示词。</p>
 */
@Getter
@Setter
@TableName("knowledge_document_wsh")
@Schema(description = "知识库文档实体")
public class KnowledgeDocument {

    @JsonProperty("id_wsh")
    @TableId(value = "id_wsh", type = IdType.AUTO)
    @Schema(description = "文档ID")
    private Long id_wsh;

    @JsonProperty("title_wsh")
    @TableField(value = "title_wsh")
    @Schema(description = "文档标题")
    private String title_wsh;

    @JsonProperty("content_wsh")
    @TableField(value = "content_wsh")
    @Schema(description = "文档内容")
    private String content_wsh;

    @JsonProperty("category_wsh")
    @TableField(value = "category_wsh")
    @Schema(description = "分类: boarding/refund/complaint/care/vaccine/agreement/rule")
    private String category_wsh;

    @JsonProperty("source_type_wsh")
    @TableField(value = "source_type_wsh")
    @Schema(description = "来源类型: pdf/word/markdown/txt")
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
