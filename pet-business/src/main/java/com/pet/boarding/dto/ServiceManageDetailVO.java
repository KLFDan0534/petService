package com.pet.boarding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 服务产品管理详情（商家/管理员可见）。
 * <p>
 * 标量字段复用 {@link ServiceItemDTO}，图册为原样的有序媒体条目
 * （含 file_id、封面标记、可信 URL），供管理端完整回显与再编辑。
 */
@Data
public class ServiceManageDetailVO {
    @Schema(description = "服务标量信息")
    private ServiceItemDTO service_wsh;
    @Schema(description = "有序图册（含文件记录ID与封面标记）")
    private List<ServiceMediaDTO> media_wsh;
}