package com.pet.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RagDocumentDTO {
    @Schema(description = "文档ID")
    private Long id_wsh;
    @Schema(description = "文档标题")
    private String title_wsh;
    @Schema(description = "文档内容")
    private String content_wsh;
    @Schema(description = "分类")
    private String category_wsh;
    @Schema(description = "来源类型")
    private String source_type_wsh;
    @Schema(description = "字数")
    private Integer word_count_wsh;
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
    @Schema(description = "更新时间")
    private LocalDateTime updated_at_wsh;
}
