package com.pet.customer.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RatingDTO {
    private Long id_wsh;
    private Long order_id_wsh;
    private Long user_id_wsh;
    private Long target_id_wsh;
    private String target_type_wsh;
    private Integer score_wsh;
    private String content_wsh;
    private String images_wsh;
    private String reply_wsh;
    private LocalDateTime reply_at_wsh;
    private LocalDateTime created_at_wsh;
}