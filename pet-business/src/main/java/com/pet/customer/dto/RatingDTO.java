package com.pet.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RatingDTO {
    @Schema(description = "评价ID")
    private Long id_wsh;
    @Schema(description = "订单ID")
    private Long order_id_wsh;
    @Schema(description = "用户ID")
    private Long user_id_wsh;
    @Schema(description = "评价目标ID")
    private Long target_id_wsh;
    @Schema(description = "评价目标类型")
    private String target_type_wsh;
    @Schema(description = "评分")
    private Integer score_wsh;
    @Schema(description = "评价内容")
    private String content_wsh;
    @Schema(description = "图片列表")
    private String images_wsh;
    @Schema(description = "回复内容")
    private String reply_wsh;
    @Schema(description = "回复时间")
    private LocalDateTime reply_at_wsh;
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
}