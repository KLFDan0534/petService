package com.pet.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatSendRequestDTO {
    @Schema(description = "接收者用户ID")
    @JsonAlias("toUserId")
    @NotNull(message = "receiver cannot be empty")
    private Long to_user_id_wsh;

    @Schema(description = "消息内容")
    @JsonAlias("content")
    private String content_wsh;

    @Schema(description = "订单ID")
    @JsonAlias("orderId")
    private Long order_id_wsh;

    @Schema(description = "消息类型")
    @JsonAlias("type")
    private String type_wsh;

    @Schema(description = "文件URL")
    @JsonAlias("fileUrl")
    private String file_url_wsh;
}
