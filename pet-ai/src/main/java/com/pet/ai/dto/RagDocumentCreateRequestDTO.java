package com.pet.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RagDocumentCreateRequestDTO {

    @NotBlank(message = "鏂囨。鏍囬涓嶈兘涓虹┖")
    @Schema(description = "鏂囨。鏍囬")
        private String title_wsh;

    @NotBlank(message = "鏂囨。鍐呭涓嶈兘涓虹┖")
    @Schema(description = "鏂囨。鍐呭")
        private String content_wsh;

    @Schema(description = "鍒嗙被")
        private String category_wsh;

    @Schema(description = "鏉ユ簮绫诲瀷")
        private String source_type_wsh;
}
