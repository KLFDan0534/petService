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

    @Value("${gao.map.js-api-key:${GAO_MAP_JS_API_KEY:}}")
    private String jsApiKey;

    @Value("${gao.map.security-code:${GAO_MAP_SECURITY_CODE:}}")
    private String securityCode;

    @Value("${gao.map.handover-radius-meters:500}")
    private int handoverRadiusMeters;

    @Value("${gao.map.attendance-radius-meters:${GAO_MAP_ATTENDANCE_RADIUS_METERS:300}}")
    private int attendanceRadiusMeters;

    /**
     * 【获取地图配置】
     *
     * 业务作用：向前端下发高德地图 API Key、安全密钥与两个距离半径配置。
     *
     * 调用场景：前端地图组件初始化时拉取配置。
     *
     * 调用链：GET /api/geo/config → GeoController.config → trimToEmpty → 组装 GeoConfigResponseDTO。
     *
     * 数据处理：四个配置项分别经 trimToEmpty 兜底后写入 DTO；enabled 由 API Key 是否为空推导。
     *
     * 业务规则：API Key 为空时 enabled=false，前端据此判断是否启用地图能力。
     *
     * 状态影响：只读。
     *
     * 异常情况：无。
     *
     * 注意事项：该接口公开可访问且直接返回明文 API Key，属于设计上的取舍；handoverRadiusMeters 未做 trimToEmpty 处理，因为它是 int。
     */
    @GetMapping("/config")
    @Operation(summary = "获取地图配置", description = "获取高德地图API密钥和相关配置")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回地图配置信息"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<GeoConfigResponseDTO> config() {
        GeoConfigResponseDTO dto = new GeoConfigResponseDTO();
        dto.setApiKey(trimToEmpty(apiKey));
        // JS API key 为空时回退到 apiKey，兼容仅配置单一 key 的部署
        String jsKey = trimToEmpty(jsApiKey);
        dto.setJsApiKey(jsKey.isEmpty() ? trimToEmpty(apiKey) : jsKey);
        dto.setSecurityCode(trimToEmpty(securityCode));
        dto.setHandoverRadiusMeters(handoverRadiusMeters);
        dto.setAttendanceRadiusMeters(attendanceRadiusMeters);
        // Key 为空时前端视为未启用地图
        dto.setEnabled(!trimToEmpty(apiKey).isEmpty());
        return Result.success(dto);
    }

    /**
     * 【去空白并兜底空串】
     *
     * 业务作用：把可空字符串 trim 后返回，null 转为空串。
     *
     * 调用场景：config 处理 apiKey 与 securityCode。
     *
     * 调用链：GeoController.config → trimToEmpty。
     *
     * 数据处理：null 返回 ""，否则返回 trim 结果。
     *
     * 业务规则：不会返回 null。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：与 AmapGeoService.trimToNull 语义相反，此处保留空串而非 null。
     */
    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
