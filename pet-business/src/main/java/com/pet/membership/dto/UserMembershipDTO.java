package com.pet.membership.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "用户会员状态")
public class UserMembershipDTO {
    private Long id_wsh;
    private Long user_id_wsh;
    private Long plan_id_wsh;
    private String plan_code_wsh;
    private String plan_name_wsh;
    private Integer level_wsh;
    private String status_wsh;
    private Boolean active_wsh;
    private LocalDateTime started_at_wsh;
    private LocalDateTime expires_at_wsh;
    private Long remaining_days_wsh;
    private Integer auto_renew_wsh;
    private String source_wsh;
    private Long last_order_id_wsh;
    private BigDecimal discount_rate_wsh;
    private String benefit_snapshot_wsh;
    private LocalDateTime created_at_wsh;
    private LocalDateTime updated_at_wsh;
}
