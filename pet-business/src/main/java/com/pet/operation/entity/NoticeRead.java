package com.pet.operation.entity;

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

/**
 * 公告阅读记录实体，记录用户对公告的阅读/关闭行为。
 * <p>
 * 采用「有记录 = 已读」的懒加载模型：用户关闭弹窗时才插入一条记录，
 * 因此不需要独立的已读状态字段，记录存在本身即代表已读。
 */
@Getter
@Setter
@TableName("notice_read_wsh")
@Schema(description = "公告阅读记录实体")
public class NoticeRead {
    @TableId(type = IdType.AUTO, value = "id_wsh")
    @Schema(description = "ID")
    private Long id_wsh;

    @TableField(value = "notice_id_wsh")
    @Schema(description = "公告ID")
    private Long notice_id_wsh;

    @TableField(value = "user_id_wsh")
    @Schema(description = "用户ID")
    private Long user_id_wsh;

    @TableField(value = "read_at_wsh")
    @Schema(description = "阅读时间")
    private LocalDateTime read_at_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    @Schema(description = "逻辑删除标志")
    private Integer deleted_wsh;

    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
}
