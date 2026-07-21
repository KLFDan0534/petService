package com.pet.operation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NoticeUpdateRequestDTO {
    @Schema(description = "公告标题")
    private String title_wsh;
    @Schema(description = "公告内容")
    private String content_wsh;
    @Schema(description = "公告类型")
    private String type_wsh;
    @Schema(description = "投递方式")
    private String delivery_type_wsh;
    @Schema(description = "图片URL")
    private String image_url_wsh;
    @Schema(description = "链接URL")
    private String link_url_wsh;
    @Schema(description = "排序值")
    private Integer sort_order_wsh;
    @Schema(description = "状态（0-草稿 1-发布）")
    private Integer status_wsh;
}
