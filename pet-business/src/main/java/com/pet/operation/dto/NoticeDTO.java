package com.pet.operation.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NoticeDTO {
    private Long id_wsh;
    private String title_wsh;
    private String content_wsh;
    private String type_wsh;
    private String image_url_wsh;
    private String link_url_wsh;
    private Integer sort_order_wsh;
    private Integer status_wsh;
    private LocalDateTime created_at_wsh;
}