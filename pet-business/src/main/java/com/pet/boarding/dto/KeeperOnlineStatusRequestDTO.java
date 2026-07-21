package com.pet.boarding.dto;

import lombok.Getter;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
public class KeeperOnlineStatusRequestDTO {
    @Schema(description = "在线状态")
    private String status_wsh;
}
