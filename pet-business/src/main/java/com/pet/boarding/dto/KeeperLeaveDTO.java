package com.pet.boarding.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
public class KeeperLeaveDTO {
    @Schema(description = "请假记录ID")
    private Long id_wsh;
    @Schema(description = "看护者ID")
    private Long keeper_id_wsh;
    @Schema(description = "商家ID")
    private Long merchant_id_wsh;
    @Schema(description = "看护者名称")
    private String keeper_name_wsh;
    @Schema(description = "商家名称")
    private String merchant_name_wsh;
    @Schema(description = "开始日期")
    private LocalDate start_date_wsh;
    @Schema(description = "结束日期")
    private LocalDate end_date_wsh;
    @Schema(description = "请假原因")
    private String reason_wsh;
    @Schema(description = "创建人")
    private Long created_by_wsh;
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
    @Schema(description = "更新时间")
    private LocalDateTime updated_at_wsh;
}
