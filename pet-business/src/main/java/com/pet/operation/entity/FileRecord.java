package com.pet.operation.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@TableName("file_record_wsh")
@Schema(description = "文件记录实体")
public class FileRecord {
    @JsonProperty("id_wsh")
    @TableId(type = IdType.AUTO, value = "id_wsh")
    @Schema(description = "ID")
    private Long id_wsh;

    @JsonProperty("original_name_wsh")
    @TableField(value = "original_name_wsh")
    @Schema(description = "原始文件名")
    private String original_name_wsh;

    @JsonProperty("object_name_wsh")
    @TableField(value = "object_name_wsh")
    @Schema(description = "存储对象名")
    private String object_name_wsh;

    @JsonProperty("size_wsh")
    @TableField(value = "size_wsh")
    @Schema(description = "文件大小(字节)")
    private Long size_wsh;

    @JsonProperty("content_type_wsh")
    @TableField(value = "content_type_wsh")
    @Schema(description = "内容类型")
    private String content_type_wsh;

    @JsonProperty("user_id_wsh")
    @TableField(value = "user_id_wsh")
    @Schema(description = "用户ID")
    private Long user_id_wsh;

    @JsonProperty("created_at_wsh")
    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;

    @JsonProperty("updated_at_wsh")
    @TableField(value = "updated_at_wsh", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updated_at_wsh;

    @Getter(AccessLevel.NONE)
    @TableField(exist = false)
    @Schema(description = "文件URL")
    private String url_wsh;

    @JsonGetter("url_wsh")
    public String getUrl_wsh() {
        return url_wsh;
    }
}
