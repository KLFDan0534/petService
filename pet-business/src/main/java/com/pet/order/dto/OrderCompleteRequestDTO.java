package com.pet.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 【确认接走（完成订单）请求】
 *
 * 业务作用：
 * keeper 在宠物实际被接走时输入独立接走码，服务端据此固定计费交接时刻并结算晚接费。
 * 与送达用的 handover_code_wsh 是两套独立凭证，不能用送达码完成订单。
 *
 * 约定：
 * - 接走码固定 4 位数字；校验保留字符串形态以保留前导 0。
 * - 不接受前端提供实际接走时刻或金额，全部由服务端计算。
 */
@Getter
@Setter
public class OrderCompleteRequestDTO {

    @Schema(description = "订单号")
    @NotBlank(message = "订单号不能为空")
    private String order_no_wsh;

    @Schema(description = "独立接走码（4 位数字）")
    @NotBlank(message = "接走码不能为空")
    private String pickup_code_wsh;
}
