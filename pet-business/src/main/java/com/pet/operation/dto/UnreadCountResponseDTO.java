package com.pet.operation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UnreadCountResponseDTO {
    @Schema(description = "未读数量")
    private long count;
}
