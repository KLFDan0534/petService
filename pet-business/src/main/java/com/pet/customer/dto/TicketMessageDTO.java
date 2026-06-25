package com.pet.customer.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TicketMessageDTO {
    private Long id_wsh;
    private Long ticket_id_wsh;
    private Long user_id_wsh;
    private String content_wsh;
    private LocalDateTime created_at_wsh;
}