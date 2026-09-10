package com.pet.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 【业务会话线程】投诉/工单维度的独立会话。
 * <p>
 * 同一用户的不同投诉/工单各自成为一条线程，客服端会话列表按业务对象区分展示，
 * 避免同一用户的多张工单/投诉混在同一个普通聊天里。
 */
@Data
public class CsThreadDTO {
    @Schema(description = "业务类型: complaint / ticket")
    private String type_wsh;
    @Schema(description = "业务ID（投诉ID或工单ID）")
    private Long biz_id_wsh;
    @Schema(description = "业务标题")
    private String title_wsh;
    @Schema(description = "对方用户ID（投诉人/工单发起人）")
    private Long other_user_id_wsh;
    @Schema(description = "对方用户名称")
    private String other_user_name_wsh;
    @Schema(description = "业务状态: pending/processing/resolved/rejected 或 ticket 状态")
    private String status_wsh;
    @Schema(description = "关联订单号")
    private String order_no_wsh;
    @Schema(description = "最近消息内容")
    private String last_message_wsh;
    @Schema(description = "最近消息时间")
    private LocalDateTime last_time_wsh;
    @Schema(description = "未读消息数（发给当前用户且未读）")
    private long unread_count_wsh;
}
