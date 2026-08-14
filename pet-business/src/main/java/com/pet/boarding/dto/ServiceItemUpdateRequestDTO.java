package com.pet.boarding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ServiceItemUpdateRequestDTO {

    @Schema(description = "服务名称")
    @Size(max = 100, message = "服务名称不能超过100个字符")
    private String name_wsh;

    @Schema(description = "分类ID")
    private Long category_id_wsh;

    @Schema(description = "服务描述")
    @Size(max = 2000, message = "服务描述不能超过2000个字符")
    private String description_wsh;

    @Schema(description = "价格")
    private BigDecimal price_wsh;

    @Schema(description = "单位（day/天 为可预约单位）")
    @Size(max = 20, message = "计费单位不能超过20个字符")
    private String unit_wsh;

    @Schema(description = "图片(逗号分隔，历史兼容字段)")
    private String images_wsh;

    @Schema(description = "状态")
    private Integer status_wsh;

    @Schema(description = "图册（非 null 时整体替换：文件ID + 排序 + 封面；空集合表示清空）")
    private List<ServiceMediaItemDTO> media_wsh;
}