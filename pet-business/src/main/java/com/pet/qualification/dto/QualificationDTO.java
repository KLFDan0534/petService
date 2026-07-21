package com.pet.qualification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QualificationDTO {
    @Schema(description = "资质ID")
    private Long id_wsh;
    @Schema(description = "所有者类型")
    private String owner_type_wsh;
    @Schema(description = "所有者ID")
    private Long owner_id_wsh;
    @Schema(description = "用户ID")
    private Long user_id_wsh;
    @Schema(description = "资质类型")
    private String qual_type_wsh;
    @Schema(description = "资质标题")
    private String title_wsh;
    @Schema(description = "资质文件URL")
    private String file_url_wsh;
    @Schema(description = "资质摘要")
    private String summary_wsh;
    @Schema(description = "审核状态")
    private String status_wsh;
    @Schema(description = "可见范围")
    private String visibility_wsh;
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
    @Schema(description = "更新时间")
    private LocalDateTime updated_at_wsh;
}
