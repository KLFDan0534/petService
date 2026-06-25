package com.pet.operation.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("operation_log_wsh")
public class OperationLog {
    @JsonProperty("id_wsh")
    @TableId(type = IdType.AUTO, value = "id_wsh")
    private Long id_wsh;

    @JsonProperty("user_id_wsh")
    @TableField(value = "user_id_wsh")
    private Long user_id_wsh;

    @JsonProperty("username_wsh")
    @TableField(value = "username_wsh")
    private String username_wsh;

    @JsonProperty("module_wsh")
    @TableField(value = "module_wsh")
    private String module_wsh;

    @JsonProperty("operation_wsh")
    @TableField(value = "operation_wsh")
    private String operation_wsh;

    @JsonProperty("description_wsh")
    @TableField(value = "description_wsh")
    private String description_wsh;

    @JsonProperty("method_wsh")
    @TableField(value = "method_wsh")
    private String method_wsh;

    @JsonProperty("request_url_wsh")
    @TableField(value = "request_url_wsh")
    private String request_url_wsh;

    @JsonProperty("request_params_wsh")
    @TableField(value = "request_params_wsh")
    private String request_params_wsh;

    @JsonProperty("request_body_wsh")
    @TableField(value = "request_body_wsh")
    private String request_body_wsh;

    @JsonProperty("response_body_wsh")
    @TableField(value = "response_body_wsh")
    private String response_body_wsh;

    @JsonProperty("ip_address_wsh")
    @TableField(value = "ip_address_wsh")
    private String ip_address_wsh;

    @JsonProperty("duration_wsh")
    @TableField(value = "duration_wsh")
    private Long duration_wsh;

    @JsonProperty("status_wsh")
    @TableField(value = "status_wsh")
    private Integer status_wsh;

    @JsonProperty("error_msg_wsh")
    @TableField(value = "error_msg_wsh")
    private String error_msg_wsh;

    @JsonProperty("created_at_wsh")
    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    private LocalDateTime created_at_wsh;
}
