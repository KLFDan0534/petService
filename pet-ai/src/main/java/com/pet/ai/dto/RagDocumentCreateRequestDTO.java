package com.pet.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RagDocumentCreateRequestDTO {

    @NotBlank(message = "文档标题不能为空")
    @Schema(description = "文档标题")
        private String title_wsh;

    @NotBlank(message = "文档内容不能为空")
    @Schema(description = "文档内容")
        private String content_wsh;

    @Schema(description = "分类")
        private String category_wsh;

    @Schema(description = "来源类型")
        private String source_type_wsh;
}
