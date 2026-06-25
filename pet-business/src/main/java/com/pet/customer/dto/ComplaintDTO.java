package com.pet.customer.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ComplaintDTO {
    private Long id_wsh;
    private Long order_id_wsh;
    private Long owner_id_wsh;
    private Long target_id_wsh;
    private String target_type_wsh;
    private String title_wsh;
    private String content_wsh;
    private String images_wsh;
    private String status_wsh;
    private String result_wsh;
    private LocalDateTime created_at_wsh;
}