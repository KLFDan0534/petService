package com.pet.geo.controller;

import com.pet.common.Result;
import com.pet.geo.dto.GeoConfigResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/geo")
@Tag(name = "【公共】地图配置", description = "高德地图配置信息")
public class GeoController {

    @Value("${gao.map.api-key:${GAO_MAP_API_KEY:}}")
    private String apiKey;

    @Value("${gao.map.security-code:${GAO_MAP_SECURITY_CODE:}}")
    private String securityCode;

    @Value("${gao.map.handover-radius-meters:500}")
    private int handoverRadiusMeters;

    @Value("${gao.map.attendance-radius-meters:${GAO_MAP_ATTENDANCE_RADIUS_METERS:300}}")
    private int attendanceRadiusMeters;

    @GetMapping("/config")
    @Operation(summary = "获取地图配置", description = "获取高德地图API密钥和相关配置")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回地图配置信息"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<GeoConfigResponseDTO> config() {
        GeoConfigResponseDTO dto = new GeoConfigResponseDTO();
        dto.setApiKey(trimToEmpty(apiKey));
        dto.setSecurityCode(trimToEmpty(securityCode));
        dto.setHandoverRadiusMeters(handoverRadiusMeters);
        dto.setAttendanceRadiusMeters(attendanceRadiusMeters);
        dto.setEnabled(!trimToEmpty(apiKey).isEmpty());
        return Result.success(dto);
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
