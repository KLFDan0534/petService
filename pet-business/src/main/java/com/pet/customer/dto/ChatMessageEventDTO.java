package com.pet.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ChatMessageEventDTO {
    @Schema(description = "消息ID")
    private Long message_id_wsh;
    @Schema(description = "订单ID")
    private Long order_id_wsh;
    @Schema(description = "发送者用户ID")
    private Long from_user_id_wsh;
    @Schema(description = "接收者用户ID")
    private Long to_user_id_wsh;
    @Schema(description = "消息类型")
    private String type_wsh;
    @Schema(description = "消息内容")
    private String content_wsh;
    @Schema(description = "图片附件URL")
    private String file_url_wsh;
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
}
