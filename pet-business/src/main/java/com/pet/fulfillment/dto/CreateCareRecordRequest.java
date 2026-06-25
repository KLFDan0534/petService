package com.pet.fulfillment.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建护理记录请求
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Data
public class CreateCareRecordRequest {
    @JsonAlias({"type", "record_type_wsh", "recordType"})
    private String type_wsh;

    @JsonAlias("content")
    private String content_wsh;

    @JsonAlias({"images", "image_urls_wsh", "imageUrls"})
    private String images_wsh;

    @JsonAlias({"recordTime", "record_time", "happened_at_wsh", "happenedAt"})
    private LocalDateTime record_time_wsh;
}
