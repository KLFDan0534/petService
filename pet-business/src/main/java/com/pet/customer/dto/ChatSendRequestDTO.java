package com.pet.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatSendRequestDTO {
    @Schema(description = "接收者用户ID")
    @NotNull(message = "receiver cannot be empty")
    private Long to_user_id_wsh;

    @Schema(description = "消息内容")
    private String content_wsh;

    @Schema(description = "订单ID")
    private Long order_id_wsh;

    @Schema(description = "消息类型")
    private String type_wsh;

    @Schema(description = "文件URL")
    private String file_url_wsh;
}
