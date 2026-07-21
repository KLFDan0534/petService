package com.pet.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

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
}
