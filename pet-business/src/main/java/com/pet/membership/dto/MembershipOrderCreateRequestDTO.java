package com.pet.membership.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "会员订单创建请求")
public class MembershipOrderCreateRequestDTO {
    @Schema(description = "会员套餐ID")
    private Long plan_id_wsh;

    @Schema(description = "会员套餐编码")
    private String plan_code_wsh;

    @Schema(description = "支付方式：balance/wechat/alipay；用户主动支付仅支持balance")
    private String pay_method_wsh;

    @NotBlank(message = "幂等请求ID不能为空")
    @Schema(description = "前端生成的幂等请求ID")
    private String request_id_wsh;

    @Schema(description = "备注")
    private String remark_wsh;
}
