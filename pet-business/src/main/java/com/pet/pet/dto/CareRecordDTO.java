package com.pet.pet.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 护理记录数据传输对象
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Data
public class CareRecordDTO {
    private Long id_wsh;
    private Long order_id_wsh;
    private Long pet_id_wsh;
    private Long keeper_id_wsh;
    private String type_wsh;
    private String content_wsh;
    private String images_wsh;
    private LocalDateTime record_time_wsh;
    private LocalDateTime created_at_wsh;
}