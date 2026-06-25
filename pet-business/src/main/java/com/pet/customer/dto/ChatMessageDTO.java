package com.pet.customer.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatMessageDTO {
    private Long id_wsh;
    private Long from_user_id_wsh;
    private Long to_user_id_wsh;
    private Long order_id_wsh;
    private String content_wsh;
    private String type_wsh;
    private String file_url_wsh;
    private Integer read_wsh;
    private LocalDateTime created_at_wsh;
}