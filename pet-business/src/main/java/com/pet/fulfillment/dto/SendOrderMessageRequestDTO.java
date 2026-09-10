package com.pet.fulfillment.dto;

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
    private Long to_user_id_wsh;

    @Schema(description = "消息内容")
    private String content_wsh;

    @Schema(description = "消息类型")
    private String type_wsh;

    @Schema(description = "文件URL")
    private String file_url_wsh;
}
