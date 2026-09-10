package com.pet.boarding.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
public class ServiceItemUpdateImagesRequestDTO {

    @Schema(description = "图片")
    @NotBlank(message = "图片URL不能为空")
        private String images_wsh;
}
