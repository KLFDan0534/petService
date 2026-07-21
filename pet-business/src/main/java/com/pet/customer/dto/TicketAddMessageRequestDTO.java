package com.pet.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TicketAddMessageRequestDTO {

    @Schema(description = "消息内容")
    @NotBlank(message = "娑堟伅鍐呭涓嶈兘涓虹┖")
        private String content_wsh;
}
