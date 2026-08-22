package com.pet.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ComplaintMessageDTO {
    @Schema(description = "消息ID")
    private Long id_wsh;
    @Schema(description = "投诉ID")
    private Long complaint_id_wsh;
    @Schema(description = "发送人ID")
    private Long from_user_id_wsh;
    @Schema(description = "发送人名称")
    private String from_user_name_wsh;
    @Schema(description = "接收人ID")
    private Long to_user_id_wsh;
    @Schema(description = "消息内容")
    private String content_wsh;
    @Schema(description = "图片附件URL")
    private String file_url_wsh;
    @Schema(description = "是否已读")
    private Integer is_read_wsh;
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
}
