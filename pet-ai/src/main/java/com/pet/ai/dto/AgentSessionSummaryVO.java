package com.pet.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 历史会话列表项。
 */
@Getter
@Setter
public class AgentSessionSummaryVO {

    @Schema(description = "会话ID")
    private String session_id_wsh;

    @Schema(description = "标题（首条用户输入截断）")
    private String title_wsh;

    @Schema(description = "COLLECTING / AWAIT_CONFIRM / CONFIRMED")
    private String status_wsh;

    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;

    @Schema(description = "最后活动时间")
    private LocalDateTime updated_at_wsh;
}
