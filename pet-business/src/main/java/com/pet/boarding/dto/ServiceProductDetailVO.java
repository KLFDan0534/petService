package com.pet.boarding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 服务产品公开详情投影（白名单）。
 * <p>
 * 仅暴露匿名用户允许查看的字段：基础信息、可信图册、评分聚合、
 * 版本与可预约性标记。不返回 raw images_wsh、内部状态、逻辑删除等字段。
 */
@Data
public class ServiceProductDetailVO {
    @Schema(description = "服务ID")
    private Long id_wsh;
    @Schema(description = "商家ID")
    private Long merchant_id_wsh;
    @Schema(description = "商家名称")
    private String merchant_name_wsh;
    @Schema(description = "服务名称")
    private String name_wsh;
    @Schema(description = "服务分类编码")
    private String type_wsh;
    @Schema(description = "分类ID")
    private Long category_id_wsh;
    @Schema(description = "分类名称")
    private String category_name_wsh;
    @Schema(description = "服务描述")
    private String description_wsh;
    @Schema(description = "价格")
    private BigDecimal price_wsh;
    @Schema(description = "计费单位")
    private String unit_wsh;
    @Schema(description = "服务版本（由更新时间生成，供下单校验）")
    private String service_version_wsh;
    @Schema(description = "服务评分（无评价为 null）")
    private BigDecimal service_rating_wsh;
    @Schema(description = "服务评价数")
    private Long service_rating_count_wsh;
    @Schema(description = "当前是否可预约（day/天 单位且商家开放未来预约）")
    private Boolean bookable_wsh;
    @Schema(description = "不可预约原因标识（可预约时为 null）")
    private String bookable_reason_wsh;
    @Schema(description = "有序图册（可信URL）")
    private List<ServiceProductMediaVO> media_wsh;
}