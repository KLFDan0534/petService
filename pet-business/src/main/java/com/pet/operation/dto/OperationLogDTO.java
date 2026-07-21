package com.pet.operation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OperationLogDTO {
    @Schema(description = "操作日志ID")
    private Long id_wsh;
    @Schema(description = "操作用户ID")
    private Long user_id_wsh;
    @Schema(description = "操作用户名")
    private String username_wsh;
    @Schema(description = "操作模块")
    private String module_wsh;
    @Schema(description = "操作类型")
    private String operation_wsh;
    @Schema(description = "操作描述")
    private String description_wsh;
    @Schema(description = "请求方法")
    private String method_wsh;
    @Schema(description = "请求URL")
    private String request_url_wsh;
    @Schema(description = "IP地址")
    private String ip_address_wsh;
    @Schema(description = "执行时长（毫秒）")
    private Long duration_wsh;
    @Schema(description = "操作状态（0-失败 1-成功）")
    private Integer status_wsh;
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
}