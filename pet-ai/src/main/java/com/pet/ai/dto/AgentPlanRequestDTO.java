package com.pet.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 智能下单 plan 阶段请求：用户自然语言描述 + 可选定位信息。
 */
@Data
public class AgentPlanRequestDTO {

    @NotBlank(message = "输入内容不能为空")
    @Size(max = 500, message = "输入内容过长，请控制在 500 字符以内")
    @Schema(description = "用户自然语言下单需求")
    private String input_wsh;

    @Schema(description = "纬度（可空，为空时回退用户档案坐标）")
    private Double latitude_wsh;

    @Schema(description = "经度（可空，为空时回退用户档案坐标）")
    private Double longitude_wsh;

    @Schema(description = "定位地址（可空，仅用于展示）")
    private String address_wsh;
}