package com.pet.boarding.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
public class KeeperLeaveCreateRequestDTO {
    @Schema(description = "看护者ID")
    @NotNull(message = "看护者不能为空")
    private Long keeper_id_wsh;

    @Schema(description = "开始日期")
    @NotNull(message = "开始日期不能为空")
    private LocalDate start_date_wsh;

    @Schema(description = "结束日期")
    @NotNull(message = "结束日期不能为空")
    private LocalDate end_date_wsh;

    @Schema(description = "请假原因")
    private String reason_wsh;
}
