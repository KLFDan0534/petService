package com.pet.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 【人工客服分配结果】智能客服转人工时随机分配的客服信息。
 */
@Data
public class ChatAgentAssignDTO {
    @Schema(description = "客服用户ID")
    private Long user_id_wsh;
    @Schema(description = "客服名称")
    private String name_wsh;
    @Schema(description = "客服头像URL")
    private String avatar_wsh;
}
