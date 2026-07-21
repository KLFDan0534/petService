package com.pet.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TicketResolveRequestDTO {

    @Schema(description = "处理结果")
        private String result_wsh;
}
