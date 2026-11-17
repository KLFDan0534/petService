package com.pet.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 一轮对话的处理结果。
 * <p>
 * {@code status_wsh}：{@code await_confirm}（方案已就绪，可确认下单）/
 * {@code collecting}（还缺信息，看 next_action）/
 * {@code failed}（模型不可用或两次都没解析出合法结构，可让用户重说）。
 */
@Getter
@Setter
public class AgentTurnResult {

    @Schema(description = "会话ID；首轮失败时为 null（不入库）")
    private String session_id_wsh;

    @Schema(description = "助手回复文本")
    private String reply_wsh;

    @Schema(description = "await_confirm / collecting / failed")
    private String status_wsh;

    @Schema(description = "下一步动作（沿用既有 next_action 枚举）")
    private String next_action_wsh;

    @Schema(description = "是否需要用户补充信息")
    private Boolean requires_user_input_wsh;

    @Schema(description = "方案卡片；仅 await_confirm 时返回")
    private AgentPlanResult plan_card;

    @Schema(description = "本轮执行日志")
    private List<String> logs;
}
