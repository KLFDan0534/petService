package com.pet.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class TicketAssignRequestDTO {

    @Schema(description = "处理人ID")
    private Long assignee_id_wsh;
}
