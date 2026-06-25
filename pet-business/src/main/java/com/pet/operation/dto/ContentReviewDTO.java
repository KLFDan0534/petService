package com.pet.operation.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ContentReviewDTO {
    private Long id_wsh;
    private String target_type_wsh;
    private Long target_id_wsh;
    private Long reporter_id_wsh;
    private String reason_wsh;
    private String status_wsh;
    private Long reviewer_id_wsh;
    private String review_remark_wsh;
    private LocalDateTime created_at_wsh;
}