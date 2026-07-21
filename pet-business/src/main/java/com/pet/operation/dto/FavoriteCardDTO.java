package com.pet.operation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FavoriteCardDTO {
    @Schema(description = "收藏记录ID")
    private Long id_wsh;
    @Schema(description = "目标ID")
    private Long target_id_wsh;
    @Schema(description = "目标类型")
    private String target_type_wsh;
    @Schema(description = "目标类型标签")
    private String target_type_label_wsh;
    @Schema(description = "收藏时间")
    private LocalDateTime created_at_wsh;
    @Schema(description = "标题")
    private String title_wsh;
    @Schema(description = "描述")
    private String description_wsh;
    @Schema(description = "图片URL")
    private String image_url_wsh;
    @Schema(description = "详情URL")
    private String detail_url_wsh;
    @Schema(description = "主要信息")
    private String primary_info_wsh;
    @Schema(description = "次要信息")
    private String secondary_info_wsh;
    @Schema(description = "金额")
    private BigDecimal amount_wsh;
    @Schema(description = "金额单位")
    private String amount_suffix_wsh;
}
