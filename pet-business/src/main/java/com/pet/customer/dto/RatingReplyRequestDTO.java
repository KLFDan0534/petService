package com.pet.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RatingReplyRequestDTO {
    @Schema(description = "回复内容")
    private String reply_wsh;
}
