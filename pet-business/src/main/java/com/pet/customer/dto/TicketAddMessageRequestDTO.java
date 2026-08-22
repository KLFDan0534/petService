package com.pet.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class TicketAddMessageRequestDTO {

    @Schema(description = "消息内容")
    private String content_wsh;

    @Schema(description = "图片附件URL")
    private String file_url_wsh;
}
