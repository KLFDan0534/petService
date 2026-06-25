package com.pet.adoption.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 领养宠物数据传输对象
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Data
public class AdoptionPetDTO {
    private Long id_wsh;
    private Long merchant_id_wsh;
    private String name_wsh;
    private String type_wsh;
    private String breed_wsh;
    private Integer age_wsh;
    private String gender_wsh;
    private BigDecimal weight_wsh;
    private String color_wsh;
    private String health_status_wsh;
    private Integer vaccinated_wsh;
    private Integer sterilized_wsh;
    private String personality_wsh;
    private String story_wsh;
    private String adoption_requirements_wsh;
    private BigDecimal adoption_fee_wsh;
    private String cover_image_wsh;
    private String images_wsh;
    private String status_wsh;
    private LocalDateTime created_at_wsh;
}