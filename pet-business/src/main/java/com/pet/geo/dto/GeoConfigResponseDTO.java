package com.pet.geo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GeoConfigResponseDTO {
    @Schema(description = "API密钥")
    private String apiKey;
    @Schema(description = "安全验证码")
    private String securityCode;
    @Schema(description = "交接半径（米）")
    private int handoverRadiusMeters;
    @Schema(description = "签到半径（米）")
    private int attendanceRadiusMeters;
    @Schema(description = "是否启用")
    private boolean enabled;
}
