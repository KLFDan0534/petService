package com.pet.boarding.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ServiceItemCreateRequestDTO {

    @Schema(description = "服务名称")
    @NotBlank(message = "服务名称不能为空")
    @Size(max = 100, message = "服务名称不能超过100个字符")
    private String name_wsh;

    @Schema(description = "服务分类ID")
    @NotNull(message = "服务分类不能为空")
    private Long category_id_wsh;

    @Schema(description = "服务描述")
    @Size(max = 2000, message = "服务描述不能超过2000个字符")
    private String description_wsh;

    @Schema(description = "价格")
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.00", message = "价格不能为负数")
    @DecimalMax(value = "1000000.00", message = "价格不能超过上限")
    private BigDecimal price_wsh;

    @Schema(description = "单位（day/天 为可预约单位，其余原样保存）")
    @Size(max = 20, message = "计费单位不能超过20个字符")
    private String unit_wsh;

    @Schema(description = "图片(逗号分隔，历史兼容字段)")
    private String images_wsh;

    @Schema(description = "图册（文件ID + 排序 + 封面；服务端校验归属与用途）")
    private List<ServiceMediaItemDTO> media_wsh;
}