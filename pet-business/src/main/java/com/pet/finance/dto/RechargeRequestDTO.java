package com.pet.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 充值请求 DTO
 * <p>用户发起充值，入账到钱包余额并记录充值流水。</p>
 *
 * @author wsh
 */
@Getter
@Setter
public class RechargeRequestDTO {
    @Schema(description = "充值金额（必须大于 0，最多两位小数）")
    private BigDecimal amount_wsh;

    @Schema(description = "幂等请求 ID（可选，缺省由服务端生成，防止重复提交）")
    private String request_id_wsh;
}