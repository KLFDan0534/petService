package com.pet.order.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Entity representing an immutable snapshot of order-related entity data.
 * <p>
 * At the moment an order is created, a snapshot is taken of all related entities
 * (owner, pet, merchant, keeper, service, address, pricing) and stored as JSON
 * blobs. This ensures that historical order records remain accurate even if the
 * referenced entities are later updated (e.g., a keeper changes their name or price).
 * <p>
 * Each snapshot field stores a JSON string containing key fields from the
 * corresponding entity at the time of order creation.
 */
@Getter
@Setter
@TableName("order_snapshot_wsh")
@Schema(description = "订单快照实体")
public class OrderSnapshot {
    @TableId(value = "id_wsh", type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id_wsh;

    @Schema(description = "订单ID")
    private Long order_id_wsh;

    @Schema(description = "订单号")
    private String order_no_wsh;

    @Schema(description = "用户快照JSON")
    private String owner_snapshot_wsh;

    @Schema(description = "宠物快照JSON")
    private String pet_snapshot_wsh;

    @Schema(description = "商家快照JSON")
    private String merchant_snapshot_wsh;

    @Schema(description = "看护者快照JSON")
    private String keeper_snapshot_wsh;

    @Schema(description = "服务快照JSON")
    private String service_snapshot_wsh;

    @Schema(description = "地址快照JSON")
    private String address_snapshot_wsh;

    @Schema(description = "价格快照JSON")
    private String price_snapshot_wsh;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
}
