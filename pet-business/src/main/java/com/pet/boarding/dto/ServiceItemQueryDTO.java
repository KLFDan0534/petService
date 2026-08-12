package com.pet.boarding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 服务列表公开查询参数。
 * <p>
 * 全部参数可选：无参数调用保持旧客户端兼容（返回全部上架服务）。
 * 排序字段走白名单，禁止客户端直接拼 SQL。
 */
@Getter
@Setter
public class ServiceItemQueryDTO {
    @Schema(description = "分类ID")
    private Long category_id_wsh;

    @Schema(description = "商家ID（商家维度列表用）")
    private Long merchant_id_wsh;

    @Schema(description = "关键字（服务名称/描述模糊匹配）")
    private String keyword_wsh;

    @Schema(description = "排序：default|price_asc|rating_desc|distance_asc")
    private String sort_wsh;

    @Schema(description = "用户纬度（配合 longitude_wsh 计算距离）")
    private BigDecimal latitude_wsh;

    @Schema(description = "用户经度")
    private BigDecimal longitude_wsh;

    @Schema(description = "页码，从 1 开始")
    private Integer page_wsh;

    @Schema(description = "每页大小 1-100，默认 20")
    private Integer size_wsh;
}