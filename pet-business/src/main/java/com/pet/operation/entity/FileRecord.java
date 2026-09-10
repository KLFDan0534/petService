package com.pet.operation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 文件上传记录实体，记录上传到 MinIO 的每个文件的元信息。
 * <p>
 * 包括原始文件名、MinIO 对象存储路径、文件大小、内容类型及所属用户。
 * 不包含文件二进制数据，文件流存储在 MinIO 中。
 */
@Getter
@Setter
@TableName("file_record_wsh")
@Schema(description = "文件记录实体")
public class FileRecord {
    @TableId(type = IdType.AUTO, value = "id_wsh")
    @Schema(description = "ID")
    private Long id_wsh;

    @TableField(value = "original_name_wsh")
    @Schema(description = "原始文件名")
    private String original_name_wsh;

    @TableField(value = "object_name_wsh")
    @Schema(description = "存储对象名")
    private String object_name_wsh;

    @TableField(value = "size_wsh")
    @Schema(description = "文件大小(字节)")
    private Long size_wsh;

    @TableField(value = "content_type_wsh")
    @Schema(description = "内容类型")
    private String content_type_wsh;

    @TableField(value = "purpose_wsh")
    @Schema(description = "文件用途: product-产品图片 avatar-头像 evidence-资质证明 其他")
    private String purpose_wsh;

    @TableField(value = "merchant_id_wsh")
    @Schema(description = "服务端归属商家ID(产品图片等受管文件)")
    private Long merchant_id_wsh;

    @TableField(value = "user_id_wsh")
    @Schema(description = "用户ID")
    private Long user_id_wsh;

    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;

    @TableField(value = "updated_at_wsh", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updated_at_wsh;

    @Getter(AccessLevel.NONE)
    @TableField(exist = false)
    @Schema(description = "文件URL")
    private String url_wsh;

    public String getUrl_wsh() {
        return url_wsh;
    }
}
