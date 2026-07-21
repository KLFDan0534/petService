package com.pet.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class MerchantCustomerServiceReviewRequestDTO {
    @Schema(description = "审核备注")
    private String review_note_wsh;
}
