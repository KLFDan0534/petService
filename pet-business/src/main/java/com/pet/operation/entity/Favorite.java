package com.pet.operation.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户收藏实体，记录用户对平台内各类目标的收藏关系。
 * <p>
 * 支持收藏的目标类型包括服务（service）、商家（merchant）、看护人（keeper）等，
 * 每种类型有独立的解析器 {@link FavoriteTargetResolver} 来提供卡片展示信息。
 */
@Getter
@Setter
@TableName("favorite_wsh")
@Schema(description = "收藏实体")
public class Favorite {
    @JsonProperty("id_wsh")
    @TableId(type = IdType.AUTO, value = "id_wsh")
    @Schema(description = "ID")
    private Long id_wsh;

    @JsonProperty("user_id_wsh")
    @TableField(value = "user_id_wsh")
    @Schema(description = "用户ID")
    private Long user_id_wsh;

    @JsonProperty("target_id_wsh")
    @TableField(value = "target_id_wsh")
    @Schema(description = "被投诉/目标对象ID")
    private Long target_id_wsh;

    @JsonProperty("target_type_wsh")
    @TableField(value = "target_type_wsh")
    @Schema(description = "被投诉/目标对象类型")
    private String target_type_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    @Schema(description = "逻辑删除标志")
    private Integer deleted_wsh;

    @JsonProperty("created_at_wsh")
    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
}
