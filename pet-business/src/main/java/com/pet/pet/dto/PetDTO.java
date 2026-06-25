package com.pet.pet.dto;

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
    private Long id_wsh;
    private Long owner_id_wsh;
    private String name_wsh;
    private String type_wsh;
    private String breed_wsh;
    private Integer age_wsh;
    private BigDecimal weight_wsh;
    private Integer gender_wsh;
    private Integer sterilized_wsh;
    private Integer vaccinated_wsh;
    private String avatar_wsh;
    private String description_wsh;
    private String allergies_wsh;
    private String habits_wsh;
    private LocalDateTime created_at_wsh;
}