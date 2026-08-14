package com.pet.boarding.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 服务产品图片条目（聚合替换请求内嵌项）。
 * <p>
 * 客户端只提交文件记录ID与排序/封面语义，绝不提交任意外部 URL。
 */
@Data
@Schema(description = "服务产品图片条目")
public class ServiceMediaItemDTO {

    @JsonProperty("file_id_wsh")
    @Schema(description = "产品用途文件记录ID(file_record_wsh.id_wsh)")
    private Long file_id_wsh;

    @JsonProperty("sort_order_wsh")
    @Schema(description = "排序序号(0..N 连续唯一)")
    private Integer sort_order_wsh;

    @JsonProperty("is_cover_wsh")
    @Schema(description = "是否封面: 0-否 1-是")
    private Integer is_cover_wsh;
}