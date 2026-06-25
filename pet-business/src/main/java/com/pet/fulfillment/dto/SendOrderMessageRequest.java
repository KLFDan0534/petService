package com.pet.fulfillment.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 发送订单消息请求
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Data
public class SendOrderMessageRequest {
    @JsonAlias({"toUserId", "to_user_id"})
    private Long to_user_id_wsh;

    @JsonAlias("content")
    private String content_wsh;

    @JsonAlias({"type", "message_type_wsh", "messageType"})
    private String type_wsh;

    @JsonAlias({"fileUrl", "file_url"})
    private String file_url_wsh;
}
