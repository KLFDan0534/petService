package com.pet.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 大模型配置（ai_config_wsh）。
 * <p>管理员在后台维护多套 AI 配置，按用途（agent=智能下单/AI助手、cs=智能客服）各启用一套。</p>
 */
@Getter
@Setter
@TableName("ai_config_wsh")
@Schema(description = "AI 配置实体")
public class AiConfigWsh {

    /** 配置用途：智能下单/AI助手 */
    public static final String USAGE_AGENT = "agent";
    /** 配置用途：智能客服 */
    public static final String USAGE_CS = "cs";

    @TableId(value = "id_wsh", type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id_wsh;

    @TableField("name_wsh")
    @Schema(description = "配置名称")
    private String name_wsh;

    @TableField("usage_wsh")
    @Schema(description = "用途: agent / cs")
    private String usage_wsh;

    @TableField("endpoint_wsh")
    @Schema(description = "OpenAI 兼容 API 基础地址")
    private String endpoint_wsh;

    @TableField("api_key_wsh")
    @Schema(description = "API Key")
    private String api_key_wsh;

    @TableField("model_wsh")
    @Schema(description = "模型名")
    private String model_wsh;

    @TableField("max_tokens_wsh")
    @Schema(description = "最大 Token 数")
    private Integer max_tokens_wsh;

    @TableField("temperature_wsh")
    @Schema(description = "温度")
    private BigDecimal temperature_wsh;

    @TableField("enabled_wsh")
    @Schema(description = "是否启用（同用途最多一条）")
    private Integer enabled_wsh;

    @TableLogic
    @TableField("deleted_wsh")
    @Schema(description = "逻辑删除")
    private Integer deleted_wsh;

    @TableField(value = "created_at_wsh", fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;

    @TableField(value = "updated_at_wsh", fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updated_at_wsh;
}