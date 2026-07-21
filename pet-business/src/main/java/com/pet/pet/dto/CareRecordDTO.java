package com.pet.pet.dto;

import com.fasterxml.jackson.annotation.JsonGetter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 护理记录数据传输对象
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Data
public class CareRecordDTO {
    @Schema(description = "护理记录ID")
    private Long id_wsh;
    @Schema(description = "订单ID")
    private Long order_id_wsh;
    @Schema(description = "宠物ID")
    private Long pet_id_wsh;
    @Schema(description = "养护人ID")
    private Long keeper_id_wsh;
    @Schema(description = "护理类型")
    private String type_wsh;
    @Schema(description = "护理内容")
    private String content_wsh;
    @Schema(description = "图片列表（JSON数组）")
    private String images_wsh;
    @Schema(description = "护理时间")
    private LocalDateTime record_time_wsh;
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;

    @JsonGetter("id")
    public Long getId() {
        return id_wsh;
    }

    @JsonGetter("content")
    public String getContent() {
        return content_wsh;
    }

    @JsonGetter("images")
    public String getImages() {
        return images_wsh;
    }
}
