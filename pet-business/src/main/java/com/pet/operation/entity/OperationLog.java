package com.pet.operation.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@TableName("operation_log_wsh")
@Schema(description = "操作日志实体")
public class OperationLog {
    @JsonProperty("id_wsh")
    @TableId(type = IdType.AUTO, value = "id_wsh")
    @Schema(description = "ID")
    private Long id_wsh;

    @JsonProperty("user_id_wsh")
    @TableField(value = "user_id_wsh")
    @Schema(description = "用户ID")
    private Long user_id_wsh;

    @JsonProperty("username_wsh")
    @TableField(value = "username_wsh")
    @Schema(description = "用户名")
    private String username_wsh;

    @JsonProperty("module_wsh")
    @TableField(value = "module_wsh")
    @Schema(description = "模块")
    private String module_wsh;

    @JsonProperty("operation_wsh")
    @TableField(value = "operation_wsh")
    @Schema(description = "操作")
    private String operation_wsh;

    @JsonProperty("description_wsh")
    @TableField(value = "description_wsh")
    @Schema(description = "描述")
    private String description_wsh;

    @JsonProperty("method_wsh")
    @TableField(value = "method_wsh")
    @Schema(description = "请求方法")
    private String method_wsh;

    @JsonProperty("request_url_wsh")
    @TableField(value = "request_url_wsh")
    @Schema(description = "请求URL")
    private String request_url_wsh;

    @JsonProperty("request_params_wsh")
    @TableField(value = "request_params_wsh")
    @Schema(description = "请求参数")
    private String request_params_wsh;

    @JsonProperty("request_body_wsh")
    @TableField(value = "request_body_wsh")
    @Schema(description = "请求体")
    private String request_body_wsh;

    @JsonProperty("response_body_wsh")
    @TableField(value = "response_body_wsh")
    @Schema(description = "响应体")
    private String response_body_wsh;

    @JsonProperty("ip_address_wsh")
    @TableField(value = "ip_address_wsh")
    @Schema(description = "IP地址")
    private String ip_address_wsh;

    @JsonProperty("duration_wsh")
    @TableField(value = "duration_wsh")
    @Schema(description = "耗时(ms)")
    private Long duration_wsh;

    @JsonProperty("status_wsh")
    @TableField(value = "status_wsh")
    @Schema(description = "状态")
    private Integer status_wsh;

    @JsonProperty("error_msg_wsh")
    @TableField(value = "error_msg_wsh")
    @Schema(description = "错误信息")
    private String error_msg_wsh;

    @JsonProperty("created_at_wsh")
    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
}
