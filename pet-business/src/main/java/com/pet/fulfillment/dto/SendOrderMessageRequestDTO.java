package com.pet.fulfillment.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 发送订单消息请求
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Data
public class SendOrderMessageRequestDTO {
    @Schema(description = "目标用户ID")
    @JsonAlias("toUserId")
    private Long to_user_id_wsh;

    @Schema(description = "消息内容")
    @JsonAlias("content")
    private String content_wsh;

    @Schema(description = "消息类型")
    @JsonAlias("type")
    private String type_wsh;

    @Schema(description = "文件URL")
    @JsonAlias("fileUrl")
    private String file_url_wsh;
}
