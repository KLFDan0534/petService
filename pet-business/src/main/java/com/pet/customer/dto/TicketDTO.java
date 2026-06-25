package com.pet.customer.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TicketDTO {
    private Long id_wsh;
    private Long user_id_wsh;
    private String title_wsh;
    private String content_wsh;
    private String category_wsh;
    private String priority_wsh;
    private String status_wsh;
    private Long assignee_id_wsh;
    private LocalDateTime created_at_wsh;
    private LocalDateTime updated_at_wsh;
}