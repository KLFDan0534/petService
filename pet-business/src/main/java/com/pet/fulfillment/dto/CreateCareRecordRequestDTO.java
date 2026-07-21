package com.pet.fulfillment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建护理记录请求
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Data
public class CreateCareRecordRequestDTO {
    @Schema(description = "护理类型")
    private String type_wsh;

    @Schema(description = "护理内容")
    private String content_wsh;

    @Schema(description = "图片列表（JSON数组）")
    private String images_wsh;

    @Schema(description = "护理时间")
    private LocalDateTime record_time_wsh;
}
