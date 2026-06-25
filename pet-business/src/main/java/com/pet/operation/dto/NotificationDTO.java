package com.pet.operation.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationDTO {
    private Long id_wsh;
    private Long user_id_wsh;
    private String title_wsh;
    private String content_wsh;
    private String type_wsh;
    private Integer is_read_wsh;
    private Long related_id_wsh;
    private LocalDateTime created_at_wsh;
}