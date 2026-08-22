package com.pet.boarding.dto;

import lombok.Data;
import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 服务项目数据传输对象
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Data
public class ServiceItemDTO {
    @Schema(description = "服务ID")
    private Long id_wsh;
    @Schema(description = "商家ID")
    private Long merchant_id_wsh;
    @Schema(description = "服务名称")
    private String name_wsh;
    @Schema(description = "服务类型")
    private String type_wsh;
    @Schema(description = "分类ID")
    private Long category_id_wsh;
    @Schema(description = "分类名称")
    private String category_name_wsh;
    @Schema(description = "服务描述")
    private String description_wsh;
    @Schema(description = "价格")
    private BigDecimal price_wsh;
    @Schema(description = "单位")
    private String unit_wsh;
    @Schema(description = "单次服务时长（分钟）")
    private Integer duration_minutes_wsh;
    @Schema(description = "预约模式（date_range/slot）")
    private String booking_mode_wsh;
    @Schema(description = "图片")
    private String images_wsh;
    @Schema(description = "状态")
    private Integer status_wsh;
    @Schema(description = "商家名称")
    private String merchant_name_wsh;
    @Schema(description = "服务评分（评分聚合，无评价为 null）")
    private BigDecimal service_rating_wsh;
    @Schema(description = "服务评价数")
    private Long service_rating_count_wsh;
    @Schema(description = "商家评分（评分聚合，无评价为 null）")
    private BigDecimal merchant_rating_wsh;
    @Schema(description = "商家评价数")
    private Long merchant_rating_count_wsh;
    @Schema(description = "距离（公里，无定位或商家无坐标时为 null）")
    private BigDecimal distance_km_wsh;
    @Schema(description = "服务版本（由服务更新时间生成，仅供变化检测）")
    private String service_version_wsh;
}
