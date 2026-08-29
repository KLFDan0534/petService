package com.pet.operation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationDTO {
    @Schema(description = "通知ID")
    private Long id_wsh;
    @Schema(description = "用户ID")
    private Long user_id_wsh;
    @Schema(description = "目标用户昵称或用户名")
    private String user_name_wsh;
    @Schema(description = "通知标题")
    private String title_wsh;
    @Schema(description = "通知内容")
    private String content_wsh;
    @Schema(description = "通知类型")
    private String type_wsh;
    @Schema(description = "是否已读（0-未读 1-已读）")
    private Integer is_read_wsh;
    @Schema(description = "关联业务ID")
    private Long related_id_wsh;
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
}