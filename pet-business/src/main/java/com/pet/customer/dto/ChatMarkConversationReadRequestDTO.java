package com.pet.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMarkConversationReadRequestDTO {
    @Schema(description = "其他用户ID")
    @NotNull(message = "other user cannot be empty")
    private Long other_user_id_wsh;

    @Schema(description = "订单ID")
    private Long order_id_wsh;
}
