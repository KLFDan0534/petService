package com.pet.operation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FavoriteDTO {
    @Schema(description = "收藏记录ID")
    private Long id_wsh;
    @Schema(description = "用户ID")
    private Long user_id_wsh;
    @Schema(description = "用户昵称或用户名")
    private String user_name_wsh;
    @Schema(description = "目标ID")
    private Long target_id_wsh;
    @Schema(description = "目标类型")
    private String target_type_wsh;
    @Schema(description = "收藏时间")
    private LocalDateTime created_at_wsh;
}