package com.pet.ai.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class KnowledgeDocumentDTO {
    private Long id_wsh;
    private String title_wsh;
    private String content_wsh;
    private String category_wsh;
    private String source_type_wsh;
    private String source_path_wsh;
    private Integer word_count_wsh;
    private LocalDateTime created_at_wsh;
}