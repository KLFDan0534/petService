package com.pet.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话详情：刷新页面 / 切换历史会话时，一次性恢复消息、状态与方案卡片。
 */
@Getter
@Setter
public class AgentSessionDetail {

    @Schema(description = "会话ID")
    private String session_id_wsh;

    @Schema(description = "标题")
    private String title_wsh;

    @Schema(description = "COLLECTING / AWAIT_CONFIRM / CONFIRMED")
    private String status_wsh;

    @Schema(description = "下一步动作")
    private String next_action_wsh;

    @Schema(description = "是否需要用户补充信息")
    private Boolean requires_user_input_wsh;

    @Schema(description = "当前方案卡片；已下单或还在收集中可能为 null")
    private AgentPlanResult plan_card;

    @Schema(description = "消息列表（按时间正序）")
    private List<AgentMessageVO> messages;

    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;

    @Schema(description = "最后活动时间")
    private LocalDateTime updated_at_wsh;
}
