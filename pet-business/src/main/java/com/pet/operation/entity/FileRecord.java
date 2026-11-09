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

    /**
     * 【读取文件访问 URL】
     *
     * 业务作用：返回非持久化字段 url_wsh 的值，供序列化时作为文件访问地址输出。
     *
     * 调用场景：文件记录被序列化为接口响应时由框架反射调用。
     *
     * 调用链：序列化框架 → FileRecord.getUrl_wsh → 返回 url_wsh 字段。
     *
     * 数据处理：直接返回字段值，无任何转换。
     *
     * 业务规则：url_wsh 标注 @TableField(exist=false)，不参与数据库读写。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：字段上的 @Getter(AccessLevel.NONE) 抑制了 Lombok 生成 getter，故需手写本方法；当前代码库中未见对 url_wsh 赋值的位置，实际调用通常返回 null，真正可用的地址由 MinIO 服务按 object_name_wsh 现算。
     */
    public String getUrl_wsh() {
        return url_wsh;
    }
}
