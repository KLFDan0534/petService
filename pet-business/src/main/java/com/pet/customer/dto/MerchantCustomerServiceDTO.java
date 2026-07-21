package com.pet.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MerchantCustomerServiceDTO {
    @Schema(description = "记录ID")
    private Long id_wsh;
    @Schema(description = "商家ID")
    private Long merchant_id_wsh;
    @Schema(description = "商家名称")
    private String merchant_name_wsh;
    @Schema(description = "用户ID")
    private Long user_id_wsh;
    @Schema(description = "用户名")
    private String username_wsh;
    @Schema(description = "昵称")
    private String nickname_wsh;
    @Schema(description = "申请备注")
    private String applicant_note_wsh;
    @Schema(description = "审核备注")
    private String review_note_wsh;
    @Schema(description = "状态")
    private String status_wsh;
    @Schema(description = "审核人ID")
    private Long reviewer_id_wsh;
    @Schema(description = "审核时间")
    private LocalDateTime reviewed_at_wsh;
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
    @Schema(description = "更新时间")
    private LocalDateTime updated_at_wsh;
}
