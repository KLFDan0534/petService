package com.pet.boarding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 服务列表分页查询结果。
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceQueryResultVO {
    @Schema(description = "当前页服务列表")
    private List<ServiceItemDTO> items_wsh;

    @Schema(description = "满足条件的服务总数")
    private long total_wsh;

    @Schema(description = "当前页码")
    private int page_wsh;

    @Schema(description = "每页大小")
    private int size_wsh;
}