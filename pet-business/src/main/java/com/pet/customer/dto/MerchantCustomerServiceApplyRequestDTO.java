package com.pet.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MerchantCustomerServiceApplyRequestDTO {
    @Schema(description = "商家ID")
    @NotNull(message = "商家ID不能为空")
    private Long merchant_id_wsh;

    @Schema(description = "申请备注")
    private String applicant_note_wsh;
}
