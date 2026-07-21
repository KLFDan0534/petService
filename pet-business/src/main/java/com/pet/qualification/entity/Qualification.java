package com.pet.qualification.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@TableName("qualification_wsh")
@Schema(description = "资质实体")
public class Qualification {
    @TableId(value = "id_wsh", type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id_wsh;

    @Schema(description = "所属者类型")
    private String owner_type_wsh;

    @Schema(description = "宠物主用户ID")
    private Long owner_id_wsh;

    @Schema(description = "用户ID")
    private Long user_id_wsh;

    @Schema(description = "资质类型")
    private String qual_type_wsh;

    @Schema(description = "标题")
    private String title_wsh;

    @Schema(description = "文件URL")
    private String file_url_wsh;

    @Schema(description = "资质摘要")
    private String summary_wsh;

    @Schema(description = "状态")
    private String status_wsh;

    @Schema(description = "可见范围")
    private String visibility_wsh;

    @Schema(description = "审核人ID")
    private Long reviewer_id_wsh;

    @Schema(description = "审核备注")
    private String review_remark_wsh;

    @JsonIgnore
    @TableLogic
    @Schema(description = "逻辑删除标志")
    private Integer deleted_wsh;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updated_at_wsh;
}
