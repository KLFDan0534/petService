package com.pet.pet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PetCreateRequestDTO {
    @Schema(description = "主人ID")
    private Long owner_id_wsh;

    @Schema(description = "宠物名称")
    @NotBlank(message = "宠物名称不能为空")
    private String name_wsh;

    @Schema(description = "宠物类型（cat/dog/other）")
    @NotBlank(message = "宠物类型不能为空")
    private String type_wsh;

    @Schema(description = "品种")
    private String breed_wsh;

    @Schema(description = "年龄（月）")
    @Positive(message = "年龄必须为正数")
    private Integer age_wsh;

    @Schema(description = "体重（kg）")
    @Positive(message = "体重必须为正数")
    private BigDecimal weight_wsh;

    @Schema(description = "性别（0-未知 1-公 2-母）")
    private Integer gender_wsh;
    @Schema(description = "是否已绝育（0-否 1-是）")
    private Integer sterilized_wsh;
    @Schema(description = "是否已打疫苗（0-否 1-是）")
    private Integer vaccinated_wsh;
    @Schema(description = "头像URL")
    private String avatar_wsh;
    @Schema(description = "宠物描述")
    private String description_wsh;
    @Schema(description = "过敏信息")
    private String allergies_wsh;
    @Schema(description = "生活习惯")
    private String habits_wsh;
}
