package com.pet.pet.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PetUpdateRequestDTO {
    @Schema(description = "宠物名称")
    @JsonAlias("name")
    private String name_wsh;
    @Schema(description = "宠物类型（cat/dog/other）")
    @JsonAlias("type")
    private String type_wsh;
    @Schema(description = "品种")
    @JsonAlias("breed")
    private String breed_wsh;

    @Schema(description = "年龄（月）")
    @Positive(message = "年龄必须为正数")
    @JsonAlias("age")
    private Integer age_wsh;

    @Schema(description = "体重（kg）")
    @Positive(message = "体重必须为正数")
    @JsonAlias("weight")
    private BigDecimal weight_wsh;

    @Schema(description = "性别（0-未知 1-公 2-母）")
    @JsonAlias("gender")
    private Integer gender_wsh;
    @Schema(description = "是否已绝育（0-否 1-是）")
    @JsonAlias("sterilized")
    private Integer sterilized_wsh;
    @Schema(description = "是否已打疫苗（0-否 1-是）")
    @JsonAlias("vaccinated")
    private Integer vaccinated_wsh;
    @Schema(description = "头像URL")
    @JsonAlias("avatar")
    private String avatar_wsh;
    @Schema(description = "宠物描述")
    @JsonAlias("description")
    private String description_wsh;
    @Schema(description = "过敏信息")
    @JsonAlias("allergies")
    private String allergies_wsh;
    @Schema(description = "生活习惯")
    @JsonAlias("habits")
    private String habits_wsh;
}
