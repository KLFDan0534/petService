package com.pet.membership.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Member plan status request")
public class MemberPlanStatusRequestDTO {
    @NotNull(message = "Plan status cannot be empty")
    @Schema(description = "0 disabled, 1 enabled")
    private Integer status_wsh;
}
