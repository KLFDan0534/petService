package com.pet.ai.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AiReportDTO {
    private Long id_wsh;
    private Long order_id_wsh;
    private Long pet_id_wsh;
    private Long keeper_id_wsh;
    private String content_wsh;
    private String type_wsh;
    private LocalDateTime created_at_wsh;
}