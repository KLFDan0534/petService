package com.pet.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class AgentExecuteResult {
    @Schema(description = "状态")
    private String status;
    @Schema(description = "订单号")
    private String orderNo;
    @Schema(description = "支付号")
    private String payNo;
    @Schema(description = "已选定的寄养人")
    private Object selectedKeeper;
    @Schema(description = "宠物名称")
    private String petName;
    @Schema(description = "天数")
    private Integer days;
    @Schema(description = "日志列表")
    private List<String> logs;
    @Schema(description = "当前步骤")
    private Integer currentStep;
    @Schema(description = "错误信息")
    private String error;
    @Schema(description = "消息")
    private String message_wsh;
    @Schema(description = "下一步动作")
    private String next_action_wsh;
    @Schema(description = "是否需要用户输入")
    private Boolean requires_user_input_wsh;
    @Schema(description = "支付状态")
    private String payment_status_wsh;

    public static AgentExecuteResult fromMap(Map<String, Object> map) {
        if (map == null) return null;
        AgentExecuteResult result = new AgentExecuteResult();
        result.setStatus((String) map.get("status"));
        result.setOrderNo((String) map.get("orderNo"));
        result.setPayNo((String) map.get("payNo"));
        result.setSelectedKeeper(map.get("selectedKeeper"));
        result.setPetName((String) map.get("petName"));
        result.setDays((Integer) map.get("days"));
        result.setLogs((List<String>) map.get("logs"));
        result.setCurrentStep((Integer) map.get("currentStep"));
        result.setError((String) map.get("error"));
        result.setMessage_wsh((String) map.get("message_wsh"));
        result.setNext_action_wsh((String) map.get("next_action_wsh"));
        result.setRequires_user_input_wsh((Boolean) map.get("requires_user_input_wsh"));
        result.setPayment_status_wsh((String) map.get("payment_status_wsh"));
        return result;
    }
}
