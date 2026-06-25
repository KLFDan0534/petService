package com.pet.operation.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FavoriteDTO {
    private Long id_wsh;
    private Long user_id_wsh;
    private Long target_id_wsh;
    private String target_type_wsh;
    private LocalDateTime created_at_wsh;
}