package com.pet.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RagDocumentUpsertRequestDTO {
    @Schema(description = "文档标题")
    @NotBlank(message = "文档标题不能为空")
    private String title_wsh;

    @Schema(description = "文档内容")
    @NotBlank(message = "文档内容不能为空")
    private String content_wsh;

    @Schema(description = "分类: boarding/refund/complaint/care/vaccine/agreement/rule")
    private String category_wsh;

    @Schema(description = "来源类型")
    private String source_type_wsh;
}
