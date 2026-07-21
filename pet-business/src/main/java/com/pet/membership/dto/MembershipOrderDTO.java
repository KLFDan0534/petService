package com.pet.membership.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "会员订单")
public class MembershipOrderDTO {
    private Long id_wsh;
    private String order_no_wsh;
    private Long user_id_wsh;
    private Long plan_id_wsh;
    private String plan_code_wsh;
    private String plan_name_wsh;
    private Integer level_wsh;
    private BigDecimal amount_wsh;
    private String pay_method_wsh;
    private String status_wsh;
    private LocalDateTime paid_at_wsh;
    private LocalDateTime membership_start_at_wsh;
    private LocalDateTime membership_end_at_wsh;
    private String request_id_wsh;
    private String plan_snapshot_wsh;
    private String remark_wsh;
    private LocalDateTime created_at_wsh;
    private LocalDateTime updated_at_wsh;
}
