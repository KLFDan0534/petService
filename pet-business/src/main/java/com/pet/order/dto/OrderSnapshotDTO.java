package com.pet.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class OrderSnapshotDTO {
    @Schema(description = "快照ID")
    private Long id_wsh;
    @Schema(description = "订单ID")
    private Long order_id_wsh;
    @Schema(description = "订单号")
    private String order_no_wsh;
    @Schema(description = "主人快照")
    private Map<String, Object> owner_snapshot_wsh;
    @Schema(description = "宠物快照")
    private Map<String, Object> pet_snapshot_wsh;
    @Schema(description = "商家快照")
    private Map<String, Object> merchant_snapshot_wsh;
    @Schema(description = "托管人快照")
    private Map<String, Object> keeper_snapshot_wsh;
    @Schema(description = "服务快照")
    private Map<String, Object> service_snapshot_wsh;
    @Schema(description = "地址快照")
    private Map<String, Object> address_snapshot_wsh;
    @Schema(description = "价格快照")
    private Map<String, Object> price_snapshot_wsh;
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
}
