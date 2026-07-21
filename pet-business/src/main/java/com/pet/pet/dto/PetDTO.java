package com.pet.pet.dto;

import com.fasterxml.jackson.annotation.JsonGetter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 宠物数据传输对象
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Data
public class PetDTO {
    @Schema(description = "宠物ID")
    private Long id_wsh;
    @Schema(description = "主人ID")
    private Long owner_id_wsh;
    @Schema(description = "宠物名称")
    private String name_wsh;
    @Schema(description = "宠物类型（cat/dog/other）")
    private String type_wsh;
    @Schema(description = "品种")
    private String breed_wsh;
    @Schema(description = "主人姓名")
    private String owner_name_wsh;
    @Schema(description = "年龄（月）")
    private Integer age_wsh;
    @Schema(description = "体重（kg）")
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
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;

    @JsonGetter("id")
    public Long getId() {
        return id_wsh;
    }

    @JsonGetter("name")
    public String getName() {
        return name_wsh;
    }

    @JsonGetter("type")
    public String getType() {
        return type_wsh;
    }

    @JsonGetter("breed")
    public String getBreed() {
        return breed_wsh;
    }
}
