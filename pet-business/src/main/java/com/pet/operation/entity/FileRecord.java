package com.pet.operation.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("file_record_wsh")
public class FileRecord {
    @JsonProperty("id_wsh")
    @TableId(type = IdType.AUTO, value = "id_wsh")
    private Long id_wsh;

    @JsonProperty("original_name_wsh")
    @TableField(value = "original_name_wsh")
    private String original_name_wsh;

    @JsonProperty("object_name_wsh")
    @TableField(value = "object_name_wsh")
    private String object_name_wsh;

    @JsonProperty("size_wsh")
    @TableField(value = "size_wsh")
    private Long size_wsh;

    @JsonProperty("content_type_wsh")
    @TableField(value = "content_type_wsh")
    private String content_type_wsh;

    @JsonProperty("user_id_wsh")
    @TableField(value = "user_id_wsh")
    private Long user_id_wsh;

    @JsonProperty("created_at_wsh")
    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    private LocalDateTime created_at_wsh;

    @JsonProperty("updated_at_wsh")
    @TableField(value = "updated_at_wsh", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updated_at_wsh;

    @Getter(AccessLevel.NONE)
    @TableField(exist = false)
    private String url_wsh;

    @JsonGetter("url_wsh")
    public String getUrl_wsh() {
        return url_wsh;
    }
}
