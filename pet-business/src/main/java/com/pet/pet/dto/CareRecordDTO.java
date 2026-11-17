package com.pet.pet.dto;

import com.pet.pet.entity.CareRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 护理记录数据传输对象
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Data
public class CareRecordDTO {
    @Schema(description = "护理记录ID")
    private Long id_wsh;
    @Schema(description = "订单ID")
    private Long order_id_wsh;
    @Schema(description = "宠物ID")
    private Long pet_id_wsh;
    @Schema(description = "养护人ID")
    private Long keeper_id_wsh;
    @Schema(description = "护理类型")
    private String type_wsh;
    @Schema(description = "护理内容")
    private String content_wsh;
    @Schema(description = "图片列表（JSON数组）")
    private String images_wsh;
    @Schema(description = "护理时间")
    private LocalDateTime record_time_wsh;
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;

    /** 实体转 DTO：控制器与履约服务共用这一份映射，避免两处各写一遍。 */
    public static CareRecordDTO from(CareRecord entity) {
        CareRecordDTO dto = new CareRecordDTO();
        dto.setId_wsh(entity.getId_wsh());
        dto.setOrder_id_wsh(entity.getOrder_id_wsh());
        dto.setPet_id_wsh(entity.getPet_id_wsh());
        dto.setKeeper_id_wsh(entity.getKeeper_id_wsh());
        dto.setType_wsh(entity.getType_wsh());
        dto.setContent_wsh(entity.getContent_wsh());
        dto.setImages_wsh(entity.getImages_wsh());
        dto.setRecord_time_wsh(entity.getRecord_time_wsh());
        dto.setCreated_at_wsh(entity.getCreated_at_wsh());
        return dto;
    }
}
