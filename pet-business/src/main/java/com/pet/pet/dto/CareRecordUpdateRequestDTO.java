package com.pet.pet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CareRecordUpdateRequestDTO {
    @Schema(description = "护理类型")
    private String type_wsh;
    @Schema(description = "护理内容")
    private String content_wsh;
    @Schema(description = "图片列表（JSON数组）")
    private String images_wsh;
    @Schema(description = "护理时间")
    private LocalDateTime record_time_wsh;
}
