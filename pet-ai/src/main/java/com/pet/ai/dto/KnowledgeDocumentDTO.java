package com.pet.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class KnowledgeDocumentDTO {
    @Schema(description = "ID")
    private Long id_wsh;
    @Schema(description = "标题")
    private String title_wsh;
    @Schema(description = "内容")
    private String content_wsh;
    @Schema(description = "分类")
    private String category_wsh;
    @Schema(description = "来源类型")
    private String source_type_wsh;
    @Schema(description = "来源路径")
    private String source_path_wsh;
    @Schema(description = "字数")
    private Integer word_count_wsh;
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
}