package com.pet.boarding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 服务产品公开详情中的单张图片。
 * <p>
 * URL 只允许来自服务端可信来源（MinIO 受管对象或解析到
 * 内部文件记录的历史值），外部/data/协议相对地址一律不下发。
 */
@Data
public class ServiceProductMediaVO {
    @Schema(description = "文件记录ID（历史兼容值无记录时为 null）")
    private Long file_id_wsh;
    @Schema(description = "排序序号")
    private Integer sort_order_wsh;
    @Schema(description = "是否封面（1=封面）")
    private Integer is_cover_wsh;
    @Schema(description = "服务端可信图片URL")
    private String url_wsh;
}