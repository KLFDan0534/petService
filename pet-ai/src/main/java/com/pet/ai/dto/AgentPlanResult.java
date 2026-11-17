package com.pet.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 智能下单方案卡片：当前会话的推荐方案（需用户确认后才落单）。
 * <p>
 * 多轮改造后不再有「方案 token」——方案归属于会话，confirm 阶段凭 {@code session_id_wsh} 下单。
 */
@Getter
@Setter
public class AgentPlanResult {

    @Schema(description = "会话ID（confirm 阶段凭此下单）")
    private String session_id_wsh;

    @Schema(description = "本轮助手回复文本")
    private String reply_wsh;

    @Schema(description = "宠物名")
    private String pet_name_wsh;
    @Schema(description = "宠物ID")
    private Long pet_id_wsh;

    @Schema(description = "商家名")
    private String merchant_name_wsh;
    @Schema(description = "商家ID")
    private Long merchant_id_wsh;

    @Schema(description = "看护人名")
    private String keeper_name_wsh;
    @Schema(description = "看护人ID")
    private Long keeper_id_wsh;

    @Schema(description = "服务名")
    private String service_name_wsh;
    @Schema(description = "服务ID")
    private Long service_id_wsh;

    @Schema(description = "计费单位（智能下单目前仅支持按夜寄养，单位 night；全局计价单位仅 night / session / hour）")
    private String unit_wsh;

    @Schema(description = "开始日期")
    private LocalDate start_date_wsh;
    @Schema(description = "结束日期")
    private LocalDate end_date_wsh;
    @Schema(description = "预订夜数（历史字段名 days_wsh，语义为夜数）")
    private Integer days_wsh;

    @Schema(description = "每夜单价（展示用）")
    private BigDecimal unit_price_wsh;
    @Schema(description = "预估总价（展示用，由服务端 /api/orders/preview 报价给出，前端不自行计算）")
    private BigDecimal total_price_wsh;

    @Schema(description = "看护人距离（公里）")
    private Double distance_wsh;

    @Schema(description = "状态：await_confirm / collecting / failed")
    private String status_wsh;
    @Schema(description = "提示消息")
    private String message_wsh;
    @Schema(description = "下一步动作")
    private String next_action_wsh;
    @Schema(description = "是否需要用户补充输入")
    private Boolean requires_user_input_wsh;
    @Schema(description = "执行日志")
    private List<String> logs;
}