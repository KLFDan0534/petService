package com.pet.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能下单会话的结构化状态，序列化后存在 agent_session_wsh.state_json_wsh。
 * <p>
 * 这是「多轮」的载体：每轮只 patch {@code intent}，未提及字段沿用旧值，
 * 再由服务端重新匹配与报价得到新的 {@code plan}。LLM 不参与价格、不参与状态判断。
 */
@Data
public class AgentSessionState {

    /** 用户意图 / 槽位（LLM 增量抽取的结果，null 表示「本轮没提」，不是「清空」） */
    @Schema(description = "用户意图槽位")
    private AgentPromptResult intent;

    /** 当前方案（字段齐全时才有值；缺字段时为 null） */
    @Schema(description = "当前方案")
    private AgentPlanSnapshot plan;

    /** COLLECTING / AWAIT_CONFIRM / CONFIRMED */
    @Schema(description = "会话状态")
    private String status;

    /** 现有 next_action 枚举，服务端确定性计算 */
    @Schema(description = "下一步动作")
    private String nextAction;

    /** 本轮助手回复，刷新页面时可直接回放 */
    @Schema(description = "本轮助手回复")
    private String reply;

    /** 每轮执行日志 */
    @Schema(description = "执行日志")
    private List<String> logs = new ArrayList<>();

    /** 首次定位后沿用，避免每轮都要求前端传坐标 */
    @Schema(description = "纬度")
    private Double latitude;

    @Schema(description = "经度")
    private Double longitude;

    @Schema(description = "定位地址")
    private String address;

    /** 本轮 LLM 修复重试次数（每轮重置，仅诊断用） */
    @Schema(description = "本轮修复重试次数")
    private Integer llmRetryCount = 0;

    /** 首条原始输入，用于会话标题与订单备注 */
    @Schema(description = "首条原始输入")
    private String firstUserInput;

    /** 最近一次解析/校验失败原因，仅诊断用 */
    @Schema(description = "最近错误原因")
    private String lastError;

    /**
     * 最近一次成功处理的本轮请求ID。
     * <p>
     * 前端每轮生成一个 {@code request_id_wsh}，超时重发时带同一个值。命中即说明这一轮
     * 已经处理并落库过，直接回放结果，不再调模型、不再写第二条消息。
     */
    @Schema(description = "最近处理的请求ID")
    private String lastRequestId;
}
