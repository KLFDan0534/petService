package com.pet.boarding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务列表公开查询参数。
 * <p>
 * 全部参数可选：无参数调用保持旧客户端兼容（返回全部上架服务）。
 * 排序字段走白名单，禁止客户端直接拼 SQL。
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
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
    private Integer page_wsh = 1;

    @Schema(description = "每页大小 1-100，默认 20")
    private Integer size_wsh = 20;

    /**
     * 生成 service-item-page 缓存 key。
     * <p>
     * 仅拼接非空字段，空字段整段跳过（不再产生 "::" 空段）；
     * 每段带字段名前缀，保证省略空段后不同参数组合仍不会互相撞 key。
     * sort/page/size 有默认值兜底，保证 key 至少非空。
     *
     * @return 形如 "mch=3:sort=default:page=1:size=100" 的缓存 key
     */
    public String cacheKey_wsh() {
        List<String> parts = new ArrayList<>();
        if (category_id_wsh != null) {
            parts.add("cat=" + category_id_wsh);
        }
        if (merchant_id_wsh != null) {
            parts.add("mch=" + merchant_id_wsh);
        }
        if (keyword_wsh != null && !keyword_wsh.isBlank()) {
            // 与 queryPublic 查询语义一致：关键字先 trim 再参与匹配
            parts.add("kw=" + keyword_wsh.trim());
        }
        parts.add("sort=" + (sort_wsh == null || sort_wsh.isBlank() ? "default" : sort_wsh));
        if (latitude_wsh != null) {
            // 归一化尾随零：39.90 与 39.9 视为同一坐标，避免同义 key 重复
            parts.add("lat=" + latitude_wsh.stripTrailingZeros().toPlainString());
        }
        if (longitude_wsh != null) {
            parts.add("lng=" + longitude_wsh.stripTrailingZeros().toPlainString());
        }
        parts.add("page=" + (page_wsh == null ? 1 : page_wsh));
        parts.add("size=" + (size_wsh == null ? 20 : size_wsh));
        return String.join(":", parts);
    }
}