package com.pet.adoption.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 领养申请数据传输对象
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Data
public class AdoptionApplicationDTO {
    private Long id_wsh;
    private Long user_id_wsh;
    private Long pet_id_wsh;
    private Long merchant_id_wsh;
    private String applicant_name_wsh;
    private String applicant_phone_wsh;
    private String applicant_address_wsh;
    private String housing_type_wsh;
    private Integer has_yard_wsh;
    private String family_members_wsh;
    private String pet_experience_wsh;
    private String reason_wsh;
    private String economic_condition_wsh;
    private Integer agree_visit_wsh;
    private String merchant_status_wsh;
    private String admin_status_wsh;
    private String status_wsh;
    private LocalDateTime created_at_wsh;
}