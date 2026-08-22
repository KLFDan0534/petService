package com.pet.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 【会话摘要】会话列表中的一项：对端用户、最近消息与未读数。
 */
@Data
public class CsConversationDTO {
    @Schema(description = "对端用户ID")
    private Long other_user_id_wsh;
    @Schema(description = "对端用户名称")
    private String other_user_name_wsh;
    @Schema(description = "最近消息内容")
    private String last_message_wsh;
    @Schema(description = "最近消息时间")
    private LocalDateTime last_time_wsh;
    @Schema(description = "未读消息数")
    private long unread_count;
}
