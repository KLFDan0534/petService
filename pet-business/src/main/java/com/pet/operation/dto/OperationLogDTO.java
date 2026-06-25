package com.pet.operation.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OperationLogDTO {
    private Long id_wsh;
    private Long user_id_wsh;
    private String username_wsh;
    private String module_wsh;
    private String operation_wsh;
    private String description_wsh;
    private String method_wsh;
    private String request_url_wsh;
    private String ip_address_wsh;
    private Long duration_wsh;
    private Integer status_wsh;
    private LocalDateTime created_at_wsh;
}