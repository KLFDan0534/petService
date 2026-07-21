package com.pet.operation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FileRecordDTO {
    @Schema(description = "文件记录ID")
    private Long id_wsh;
    @Schema(description = "文件访问URL")
    private String url_wsh;
    @Schema(description = "文件原始名称")
    private String original_name_wsh;
    @Schema(description = "文件类型")
    private String file_type_wsh;
    @Schema(description = "文件大小（字节）")
    private Long file_size_wsh;
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
}
