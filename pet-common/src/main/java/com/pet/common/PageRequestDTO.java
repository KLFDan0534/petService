package com.pet.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * 校验规则：
 * - page 最小为 1
 * - size 范围 1~100
 *
 * 业务作用：统一分页查询的请求参数封装，所有分页接口继承/使用此 DTO。 提供默认值：page=1, size=10，前端可选择性覆盖。
 */
@Getter
@Setter
public class PageRequestDTO {
    @Schema(description = "页码，从1开始")
    @Min(value = 1, message = "每页条数最小为1")
    private int page = 1;

    @Schema(description = "每页条数，最大100")
    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    private int size = 10;

    @Schema(description = "排序字段，如 id / balance / created")
    private String sort_by_wsh;

    @Schema(description = "排序方向，asc / desc")
    private String order_wsh;
}
