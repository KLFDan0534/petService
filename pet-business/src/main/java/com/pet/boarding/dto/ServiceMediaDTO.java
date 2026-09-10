package com.pet.boarding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 服务产品图片展示 DTO（包含按服务端可信来源推导的公开 URL）。
 */
@Data
@Schema(description = "服务产品图片展示")
public class ServiceMediaDTO {

    @Schema(description = "媒体行ID")
    private Long id_wsh;

    @Schema(description = "文件记录ID")
    private Long file_id_wsh;

    @Schema(description = "排序序号")
    private Integer sort_order_wsh;

    @Schema(description = "是否封面: 0-否 1-是")
    private Integer is_cover_wsh;

    @Schema(description = "公开访问URL(服务端可信来源推导)")
    private String url_wsh;

    @Schema(description = "原始文件名")
    private String original_name_wsh;

    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
}